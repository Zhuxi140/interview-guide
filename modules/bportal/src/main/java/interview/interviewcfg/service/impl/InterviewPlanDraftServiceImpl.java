package interview.interviewcfg.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.interviewcfg.mapper.InterviewPlanDraftMapper;
import interview.interviewcfg.model.entity.InterviewPlanDraft;
import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import interview.interviewcfg.model.req.InterviewPlanDraftApplyReq;
import interview.interviewcfg.model.req.InterviewPlanDraftCreateReq;
import interview.interviewcfg.model.vo.InterviewPlanDraftApplyVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftCreateVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftDetailVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftListItemVO;
import interview.interviewcfg.service.InterviewPlanDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 面试编排草案实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewPlanDraftServiceImpl extends ServiceImpl<InterviewPlanDraftMapper, InterviewPlanDraft>
        implements InterviewPlanDraftService {

    private final EnterpriseValidationApi enterpriseValidationApi;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewPlanDraftCreateVO createDraft(Long enterpriseId, Long applicationId,
                                                   String idempotencyKey, InterviewPlanDraftCreateReq req) {
        // TODO ① 从 AuthContext 获取发起人，校验企业成员身份、接口权限和 Idempotency-Key 非空。
        // TODO ② 通过投递模块校验 applicationId 属于 enterpriseId、状态允许进入面试，并取得岗位和候选人快照。
        // TODO ③ 校验第三阶段 interviewType 只能为 TEXT；templateId 必填且属于当前企业，候选面试官必须是企业成员。
        // TODO ④ 按企业+投递+发起人+幂等键查询历史草案；相同请求返回原结果，不同请求复用键返回幂等冲突。
        // TODO ⑤ 读取模板及全部阶段配置，检查阶段顺序，并把模板 ID、版本和完整阶段配置写入 inputSnapshotJson。
        // TODO ⑥ 检查同一投递没有 PENDING/PROCESSING/READY 活动草案；约束 Agent 只能在快照阶段内生成考察点、题纲和排期建议。
        // TODO ⑦ 在同一事务插入 PENDING 草案和 Agent 生成 Outbox 消息，保存 generationMessageId；禁止远程写中央消息表破坏本地事务。
        // TODO ⑧ 事务提交后立即派发消息；Handler 原子领取草案、调用 Agent，并按执行栅栏写入 READY 或 FAILED。
        // TODO ⑨ 模型调用失败由消息租约和有限重试收敛，重复消费不得重复计费或覆盖新一轮执行结果。
        // TODO ⑩ 返回 draftId、applicationId、PENDING 和初始 version，Controller 使用 HTTP 202。
        return null;
    }

    @Override
    @Transactional
    public InterviewPlanDraftApplyVO applyDraft(Long enterpriseId, Long applicationId,
                                                 Long draftId, String idempotencyKey,
                                                InterviewPlanDraftApplyReq req) {
        // ② 规一化提交计划，先计算一次供各分支复用。
        String normalizedPlan = normalizePlan(req.plan());

        // ③ 读取草案（仅取幂等与状态机所需列），锁定租户与投递范围。
        InterviewPlanDraft draft = baseMapper.selectOne(Wrappers.<InterviewPlanDraft>lambdaQuery()
                .select(
                        InterviewPlanDraft::getId, InterviewPlanDraft::getStatus,
                        InterviewPlanDraft::getVersion, InterviewPlanDraft::getExpiresAt,
                        InterviewPlanDraft::getPlanJson, InterviewPlanDraft::getApplyIdempotencyKey,
                        InterviewPlanDraft::getAppliedPlanJson, InterviewPlanDraft::getAppliedScheduleIds,
                        InterviewPlanDraft::getAppliedAt)
                .eq(InterviewPlanDraft::getId, draftId)
                .eq(InterviewPlanDraft::getEnterpriseId, enterpriseId)
                .eq(InterviewPlanDraft::getApplicationId, applicationId));
        if (draft == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_PLAN_DRAFT_NOT_FOUND);
        }
        if (draft.getStatus() == InterviewPlanDraftStatus.APPLIED) {
            // 读到即为终态：同键同计划为幂等重放，其余一律拒绝。
            return resolveReplayOrConflict(idempotencyKey, normalizedPlan, draft);
        }
        if (draft.getStatus() != InterviewPlanDraftStatus.READY) {
            throw new BusinessException(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED);
        }
        if (draft.getExpiresAt() != null && draft.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED);
        }

        // ④ 校验 HR 修订版计划：阶段结构与顺序必须与草案快照一致（白名单，防绕过模板约束）。
        validateStageStructure(draft.getPlanJson(), req.plan());

        // TODO [Scheduling] 校验选中建议的面试官成员身份、时间合法性与冲突后，
        //  在事务内按 roundNo=1..N 创建排期并回填 appliedScheduleIds；当前暂返回空结果。

        // ⑤ CAS 认领：仅 READY + 版本匹配可置 APPLIED，失败说明已被并发请求处理。
        InterviewPlanDraft update = new InterviewPlanDraft();
        update.setId(draftId);
        update.setEnterpriseId(enterpriseId);
        update.setStatus(InterviewPlanDraftStatus.APPLIED);
        update.setApplyIdempotencyKey(idempotencyKey);
        update.setAppliedPlanJson(normalizedPlan);
        update.setAppliedScheduleIds("[]");
        update.setAppliedAt(OffsetDateTime.now());
        update.setUpdatedBy(AuthContext.getRequiredUserId());
        update.setVersion(draft.getVersion());
        int affected = baseMapper.update(update, Wrappers.<InterviewPlanDraft>lambdaUpdate()
                .eq(InterviewPlanDraft::getId, draftId)
                .eq(InterviewPlanDraft::getEnterpriseId, enterpriseId)
                .eq(InterviewPlanDraft::getApplicationId, applicationId)
                .eq(InterviewPlanDraft::getStatus, InterviewPlanDraftStatus.READY));
        if (affected != 1) {
            // 并发输家：行锁释放后重读最新状态（仅需幂等判定与结果快照列），同键同计划视为重放，否则报业务冲突。
            InterviewPlanDraft latest = baseMapper.selectOne(Wrappers.<InterviewPlanDraft>lambdaQuery()
                    .select(
                            InterviewPlanDraft::getId, InterviewPlanDraft::getStatus,
                            InterviewPlanDraft::getApplyIdempotencyKey, InterviewPlanDraft::getAppliedPlanJson,
                            InterviewPlanDraft::getAppliedScheduleIds, InterviewPlanDraft::getAppliedAt)
                    .eq(InterviewPlanDraft::getId, draftId)
                    .eq(InterviewPlanDraft::getEnterpriseId, enterpriseId)
                    .eq(InterviewPlanDraft::getApplicationId, applicationId));
            if (latest == null || latest.getStatus() != InterviewPlanDraftStatus.APPLIED) {
                throw new BusinessException(ErrorCode.INTERVIEW_PLAN_DRAFT_VERSION_CONFLICT);
            }
            return resolveReplayOrConflict(idempotencyKey, normalizedPlan, latest);
        }
        return buildApplyVO(update);
    }

    @Override
    public IPage<InterviewPlanDraftListItemVO> pageDrafts(Long enterpriseId,
                                                           Long applicationId,
                                                           Integer page,
                                                           Integer size,
                                                           InterviewPlanDraftStatus status,
                                                           String sort,
                                                           String order) {
        // 校验当前企业归属及分页排序参数，避免绕过 Controller 直接调用时产生越权或任意排序。
        enterpriseValidationApi.validateActiveEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        validatePage(page, size);
        validateSort(sort);
        validateOrder(order);

        // 按企业和投递隔离查询，状态条件可选。
        boolean ascending = "asc".equalsIgnoreCase(order);
        LambdaQueryChainWrapper<InterviewPlanDraft> query = lambdaQuery()
                .select(
                        InterviewPlanDraft::getId, InterviewPlanDraft::getApplicationId,
                        InterviewPlanDraft::getTemplateId, InterviewPlanDraft::getRequestJson,
                        InterviewPlanDraft::getStatus, InterviewPlanDraft::getFailureReason,
                        InterviewPlanDraft::getVersion, InterviewPlanDraft::getCreatedAt,
                        InterviewPlanDraft::getUpdatedAt
                )
                .eq(InterviewPlanDraft::getEnterpriseId, enterpriseId)
                .eq(InterviewPlanDraft::getApplicationId, applicationId)
                .eq(status != null, InterviewPlanDraft::getStatus, status);
        switch (sort) {
            case "createdAt" -> query.orderBy(true, ascending, InterviewPlanDraft::getCreatedAt);
            case "updatedAt" -> query.orderBy(true, ascending, InterviewPlanDraft::getUpdatedAt);
            case "id" -> query.orderBy(true, ascending, InterviewPlanDraft::getId);
            default -> throw new BusinessException(ErrorCode.SORT_FIELD_INVALID);
        }
        if (!"id".equals(sort)) {
            query.orderBy(true, ascending, InterviewPlanDraft::getId);
        }

        // 请求快照中的 interviewType 是列表契约的一部分，统一解析后映射 VO。
        return query.page(new Page<>(page, size))
                .convert(draft -> new InterviewPlanDraftListItemVO(
                        draft.getId(),
                        draft.getApplicationId(),
                        draft.getTemplateId(),
                        parseInterviewType(draft.getRequestJson()),
                        draft.getStatus(),
                        draft.getFailureReason(),
                        draft.getVersion(),
                        draft.getCreatedAt(),
                        draft.getUpdatedAt()
                ));
    }

    @Override
    public InterviewPlanDraftDetailVO getDraftDetail(Long enterpriseId,
                                                      Long applicationId,
                                                      Long draftId) {
        // 使用企业、投递和草案三重条件完成租户隔离。
        enterpriseValidationApi.validateActiveEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        InterviewPlanDraft draft = lambdaQuery()
                .select(
                        InterviewPlanDraft::getId,
                        InterviewPlanDraft::getApplicationId,
                        InterviewPlanDraft::getStatus,
                        InterviewPlanDraft::getFailureReason,
                        InterviewPlanDraft::getPlanJson,
                        InterviewPlanDraft::getAppliedPlanJson,
                        InterviewPlanDraft::getVersion,
                        InterviewPlanDraft::getCreatedAt
                )
                .eq(InterviewPlanDraft::getId, draftId)
                .eq(InterviewPlanDraft::getEnterpriseId, enterpriseId)
                .eq(InterviewPlanDraft::getApplicationId, applicationId)
                .one();
        if (draft == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_PLAN_DRAFT_NOT_FOUND);
        }

        // 已应用的草案返回 HR 提交的修订版计划，保证页面刷新后编辑状态可恢复；
        // 其余可展示状态返回 Agent 原始计划。
        InterviewPlanDraftDetailVO.PlanVO plan = null;
        if (draft.getStatus() == InterviewPlanDraftStatus.APPLIED
                && draft.getAppliedPlanJson() != null && !draft.getAppliedPlanJson().isBlank()) {
            plan = parsePlan(draft.getAppliedPlanJson());
        } else if (draft.getStatus() == InterviewPlanDraftStatus.READY) {
            plan = parsePlan(draft.getPlanJson());
        }
        return new InterviewPlanDraftDetailVO(
                draft.getId(),
                draft.getApplicationId(),
                draft.getStatus(),
                draft.getFailureReason(),
                plan,
                draft.getVersion(),
                draft.getCreatedAt()
        );
    }

    private InterviewType parseInterviewType(String requestJson) {
        try {
            return objectMapper.readValue(
                    requestJson, InterviewPlanDraftCreateReq.class).interviewType();
        } catch (Exception exception) {
            log.error("面试编排草案请求快照无法解析", exception);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private InterviewPlanDraftDetailVO.PlanVO parsePlan(String planJson) {
        if (planJson == null || planJson.isBlank()) {
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
        try {
            return objectMapper.readValue(
                    planJson, InterviewPlanDraftDetailVO.PlanVO.class);
        } catch (Exception exception) {
            log.error("面试编排草案计划无法解析", exception);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private void validateStageStructure(String snapshotPlanJson, InterviewPlanDraftApplyReq.Plan submitted) {
        // 阶段编码集合与顺序是组卷、出题和排期唯一约束的锚点，不允许客户端变更。
        InterviewPlanDraftDetailVO.PlanVO snapshot = parsePlan(snapshotPlanJson);
        List<String> expected = snapshot.stages().stream()
                .map(InterviewPlanDraftDetailVO.StagePlanVO::phaseCode)
                .toList();
        List<String> actual = submitted.stages().stream()
                .map(InterviewPlanDraftApplyReq.Stage::phaseCode)
                .toList();
        if (!expected.equals(actual)) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_STAGE_INVALID);
        }
    }

    private InterviewPlanDraftApplyVO buildApplyVO(InterviewPlanDraft draft) {
        return new InterviewPlanDraftApplyVO(
                draft.getId(),
                InterviewPlanDraftStatus.APPLIED,
                parseScheduleIds(draft.getAppliedScheduleIds()),
                draft.getAppliedAt());
    }

    private InterviewPlanDraftApplyVO resolveReplayOrConflict(String idempotencyKey,
                                                              String normalizedPlan,
                                                              InterviewPlanDraft draft) {
        // 已应用：同键同计划为幂等重放，返回原结果；其余一律拒绝。
        if (idempotencyKey.equals(draft.getApplyIdempotencyKey())
                && samePlan(normalizedPlan, draft.getAppliedPlanJson())) {
            return buildApplyVO(draft);
        }
        throw new BusinessException(ErrorCode.INTERVIEW_PLAN_DRAFT_ALREADY_APPLIED);
    }

    private List<Long> parseScheduleIds(String appliedScheduleIds) {
        if (appliedScheduleIds == null || appliedScheduleIds.isBlank()) {
            return List.of();
        }
        try {
            List<Long> ids = new ArrayList<>();
            JsonNode array = objectMapper.readTree(appliedScheduleIds);
            if (array.isArray()) {
                array.forEach(node -> ids.add(node.asLong()));
            }
            return ids;
        } catch (Exception exception) {
            log.error("面试编排草案已应用排期快照无法解析", exception);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private String normalizePlan(InterviewPlanDraftApplyReq.Plan plan) {
        try {
            return objectMapper.writeValueAsString(objectMapper.valueToTree(plan));
        } catch (Exception exception) {
            log.error("面试编排草案应用计划序列化失败", exception);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private boolean samePlan(String submitted, String stored) {
        if (stored == null || stored.isBlank()) {
            return false;
        }
        try {
            return objectMapper.readTree(submitted).equals(objectMapper.readTree(stored));
        } catch (Exception exception) {
            log.error("面试编排草案应用计划对比失败", exception);
            return false;
        }
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private void validateSort(String sort) {
        if (!"createdAt".equals(sort)
                && !"updatedAt".equals(sort)
                && !"id".equals(sort)) {
            throw new BusinessException(ErrorCode.SORT_FIELD_INVALID);
        }
    }

    private void validateOrder(String order) {
        if (!"asc".equalsIgnoreCase(order) && !"desc".equalsIgnoreCase(order)) {
            throw new BusinessException(ErrorCode.SORT_DIRECTION_INVALID);
        }
    }
}
