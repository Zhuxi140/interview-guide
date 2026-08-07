package interview.interviewcfg.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.api.aicore.dto.JobApplicationSnapshotDTO;
import interview.api.bportal.JobValidationApi;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewType;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.common.exception.BusinessException;
import interview.common.util.IdGeneratorUtil;
import interview.framework.config.CustomIdGenerator;
import interview.framework.context.AuthContext;
import interview.interviewcfg.event.AgentPlanDraftEvent;
import interview.interviewcfg.mapper.InterviewPlanDraftMapper;
import interview.interviewcfg.model.bo.InterviewTemplateSnapshot;
import interview.interviewcfg.model.entity.InterviewPlanDraft;
import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import interview.interviewcfg.model.req.InterviewPlanDraftApplyReq;
import interview.interviewcfg.model.req.InterviewPlanDraftCreateReq;
import interview.interviewcfg.model.req.InterviewScheduleCreateReq;
import interview.interviewcfg.model.vo.InterviewPlanDraftApplyVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftCreateVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftDetailVO;
import interview.interviewcfg.model.vo.InterviewPlanDraftListItemVO;
import interview.interviewcfg.model.vo.InterviewScheduleCreateVO;
import interview.interviewcfg.service.InterviewPlanDraftService;
import interview.interviewcfg.service.InterviewScheduleService;
import interview.interviewcfg.service.InterviewStageTemplateService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agent 面试编排草案实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewPlanDraftServiceImpl extends ServiceImpl<InterviewPlanDraftMapper, InterviewPlanDraft>
        implements InterviewPlanDraftService {

    private static final String GENERATION_BIZ_PREFIX = "INTERVIEW_PLAN_GENERATION:";
    private static final int GENERATION_TIMEOUT_SECONDS = 300;
    private static final DateTimeFormatter ISO_TIME_WITH_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final EnterpriseValidationApi enterpriseValidationApi;
    private final JobValidationApi jobValidationApi;
    private final InterviewStageTemplateService interviewStageTemplateService;
    private final InterviewScheduleService interviewScheduleService;
    private final ApplicationEventPublisher eventPublisher;
    private final LocalMessageApi localMessageApi;
    private final CustomIdGenerator customIdGenerator;


    @Override
    @Transactional
    public InterviewPlanDraftCreateVO createDraft(Long enterpriseId, Long applicationId,
                                                   String idempotencyKey, InterviewPlanDraftCreateReq req) {
        //  通过投递模块校验 applicationId 属于 enterpriseId、状态允许进入面试，并取得岗位和候选人快照。
        JobApplicationSnapshotDTO jobApplicationSnapshotDTO = jobValidationApi.requirePassedApplication(enterpriseId, applicationId);
        // 校验面试官与模板归属：模板必须属于当前企业。
        // interviewType 校验：本阶段仅接受 TEXT——待 Phase 07（代码与语音面试）语音排期上线后放开为 TEXT/VOICE。
        //       注意：草案按 requestJson.interviewType 整单单选，无法表达「R1 文本 + R2 语音」的轮次混排；
        //       放开语音时需在 CreateRequest 与 plan.scheduleSuggestions 补充 per-round interviewType 并调整 apply 透传（见阶段三接口文档.md：87 行 Phase 5 扩展点）。
        if (! req.interviewType().equals(InterviewType.TEXT)) {
            throw new BusinessException(ErrorCode.INTERVIEW_TYPE_NOT_ALLOWED);
        }
        // 免鉴权生成模板快照（调用方已通过投递模块校验企业归属），同时约束 Agent 只能在快照阶段内生成考察点、题纲和排期建议。
        InterviewTemplateSnapshot templateSnapshot =
                interviewStageTemplateService.buildTemplateSnapshotWithoutAuth(enterpriseId, req.templateId());
        if (templateSnapshot == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_STAGE_TEMPLATE_NOT_FOUND);
        }
        // HR 可选定有序阶段子集：快照按子集裁剪后再交给 Agent，保证「快照即 Agent 生成边界」成立，无需在执行校验层二次换算。
        InterviewTemplateSnapshot planSnapshot = filterSnapshotStages(templateSnapshot, req);
        String inputSnapshotJson = writeSnapshotJson(planSnapshot);

        // 候选面试官必须是企业成员
        List<Long>  interviewUserIds = req.interviewerUserIds().isEmpty() ? null : req.interviewerUserIds().stream().distinct().toList();
        if (interviewUserIds != null){
            enterpriseValidationApi.validateEnterpriseMembers(enterpriseId,interviewUserIds);
        }

        // 按企业+投递+发起人+幂等键查询历史草案；相同请求返回原结果，不同请求复用键返回幂等冲突。
        Long userId = AuthContext.getRequiredUserId();
        InterviewPlanDraft draft = lambdaQuery()
                .select(
                        InterviewPlanDraft::getId, InterviewPlanDraft::getApplicationId,
                        InterviewPlanDraft::getStatus, InterviewPlanDraft::getVersion)
                .eq(InterviewPlanDraft::getEnterpriseId, enterpriseId)
                .eq(InterviewPlanDraft::getApplicationId, applicationId)
                .eq(InterviewPlanDraft::getRequestedBy,userId)
                .eq(InterviewPlanDraft::getIdempotencyKey, idempotencyKey)
                .one();
        if (draft != null) {
            return new InterviewPlanDraftCreateVO(draft.getId(), draft.getApplicationId(), draft.getStatus(), draft.getVersion());
        }
        // 检查同一投递没有 PENDING/PROCESSING/READY 活动草案；约束 Agent 只能在快照阶段内生成考察点、题纲和排期建议。
        boolean exists = lambdaQuery()
                .eq(InterviewPlanDraft::getEnterpriseId, enterpriseId)
                .eq(InterviewPlanDraft::getApplicationId, applicationId)
                .in(InterviewPlanDraft::getStatus,
                        InterviewPlanDraftStatus.PENDING,
                        InterviewPlanDraftStatus.PROCESSING,
                        InterviewPlanDraftStatus.READY)
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED);
        }
        // 先保存本地生成消息取得 messageId，再一次性插入 PENDING 草案（含 generationMessageId），省去一次 UPDATE。
        try {
            Long draftId = (Long) customIdGenerator.nextId(InterviewPlanDraft.class);
            OffsetDateTime now = OffsetDateTime.now();
            MessageDTO message = MessageDTO.builder()
                    .bizKey(GENERATION_BIZ_PREFIX + draftId)
                    .topic(MsgTopic.INTERVIEW_PLAN_GENERATION)
                    .schemaVersion(1)
                    .payload(String.valueOf(draftId))
                    .status(MsgStatus.PENDING)
                    .priority(MsgPriority.HIGH)
                    .maxRetries(5)
                    .nextRetryAt(now.plusSeconds(GENERATION_TIMEOUT_SECONDS))
                    .build();
            Long messageId = localMessageApi.saveInCurrentTransaction(message);

            OffsetDateTime expiresAt = now.plusDays(DRAFT_VALIDITY_DAYS);
            InterviewPlanDraft newDraft = InterviewPlanDraft.builder()
                    .id(draftId)
                    .enterpriseId(enterpriseId)
                    .applicationId(applicationId)
                    .templateId(req.templateId())
                    .requestJson(writeRequestJson(req))
                    .inputSnapshotJson(inputSnapshotJson)
                    .status(InterviewPlanDraftStatus.PENDING)
                    .idempotencyKey(idempotencyKey)
                    .requestedBy(userId)
                    .generationMessageId(messageId)
                    .version(0)
                    .expiresAt(expiresAt)
                    .build();
            save(newDraft);

            eventPublisher.publishEvent(new AgentPlanDraftEvent(
                    messageId, draftId, enterpriseId, applicationId));
            // ⑩ 返回 draftId、applicationId、PENDING 和初始 version，Controller 使用 HTTP 202。
            return new InterviewPlanDraftCreateVO(
                    draftId, applicationId, InterviewPlanDraftStatus.PENDING, 0);
        } catch (DuplicateKeyException ignored) {
            // 并发同键：PostgreSQL 事务已中止，本事务内不可再查询；统一按幂等冲突拒绝，客户端重试命中幂等查询后拿到原结果。
            throw new BusinessException(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED);
        }
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
                        InterviewPlanDraft::getTemplateId, InterviewPlanDraft::getInputSnapshotJson,
                        InterviewPlanDraft::getPlanJson,
                        InterviewPlanDraft::getApplyIdempotencyKey,
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

        // ④ 校验草案基于的模板版本仍是当前版本；模板变更后旧草案不可落地，需重新生成。
        validateCurrentTemplateVersion(enterpriseId, draft);

        // ⑤ 校验 HR 修订版计划：阶段结构与顺序必须与草案快照一致（白名单，防绕过模板约束）。
        validateStageStructure(draft.getPlanJson(), req.plan());

        // ⑤ 排期建议与阶段一一对应（每轮一条，可为空表示本轮不建排期）；复用 createSchedule
        // 的全部校验（租户归属、投递状态、面试官成员身份、时间合法性、日历冲突、轮次连续性），
        // 按 roundNo=1..N 在事务内创建排期。
        List<Long> createdScheduleIds = createScheduleFromPlan(
                enterpriseId, applicationId, draft, req.plan(), idempotencyKey);

        // ⑤ CAS 认领：仅 READY + 版本匹配可置 APPLIED，失败说明已被并发请求处理。
        InterviewPlanDraft update = new InterviewPlanDraft();
        update.setId(draftId);
        update.setEnterpriseId(enterpriseId);
        update.setStatus(InterviewPlanDraftStatus.APPLIED);
        update.setApplyIdempotencyKey(idempotencyKey);
        update.setAppliedPlanJson(normalizedPlan);
        update.setAppliedScheduleIds(serializeScheduleIds(createdScheduleIds));
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
            return InterviewType.valueOf(JSONUtil.parseObj(requestJson).getStr("interviewType"));
        } catch (Exception exception) {
            log.error("面试编排草案请求快照无法解析", exception);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private InterviewTemplateSnapshot filterSnapshotStages(
            InterviewTemplateSnapshot templateSnapshot, InterviewPlanDraftCreateReq req) {
        List<String> phaseCodes = req.phaseCodes();
        if (phaseCodes == null || phaseCodes.isEmpty()) {
            return templateSnapshot;
        }
        // 阶段编码必须存在于模板且保持 HR 指定顺序，超 maxRounds 的请求提前拦截，避免 AI 阶段数与模板快照对不上。
        Map<String, InterviewTemplateSnapshot.StageSnapshot> byCode = new java.util.LinkedHashMap<>();
        for (InterviewTemplateSnapshot.StageSnapshot stage : templateSnapshot.stages()) {
            byCode.put(stage.phaseCode(), stage);
        }
        List<InterviewTemplateSnapshot.StageSnapshot> selected = new ArrayList<>();
        for (String code : phaseCodes) {
            InterviewTemplateSnapshot.StageSnapshot stage = byCode.get(code);
            if (stage == null) {
                throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_STAGE_INVALID);
            }
            selected.add(stage);
        }
        if (req.maxRounds() != null && selected.size() > req.maxRounds()) {
            throw new BusinessException(ErrorCode.INTERVIEW_FLOW_NOT_ALLOWED);
        }
        return new InterviewTemplateSnapshot(
                templateSnapshot.templateId(),
                templateSnapshot.templateVersion(),
                templateSnapshot.templateName(),
                selected);
    }

    private String writeRequestJson(InterviewPlanDraftCreateReq req) {
        try {
            JSONObject json = new JSONObject();
            json.put("templateId", req.templateId());
            json.put("interviewType", req.interviewType());
            json.put("durationMinutes", req.durationMinutes());
            json.put("maxRounds", req.maxRounds());
            json.put("interviewerUserIds", req.interviewerUserIds());
            json.put("phaseCodes", req.phaseCodes());
            json.put("prompt", req.prompt());
            return json.toString();
        } catch (Exception exception) {
            log.error("面试编排草案请求快照序列化失败", exception);
            throw new BusinessException(ErrorCode.OBJECT_TO_JSON_ERROR);
        }
    }

    private String writeSnapshotJson(InterviewTemplateSnapshot snapshot) {
        try {
            JSONObject root = new JSONObject();
            root.put("templateId", snapshot.templateId());
            root.put("templateVersion", snapshot.templateVersion());
            root.put("templateName", snapshot.templateName());
            JSONArray stages = new JSONArray();
            for (InterviewTemplateSnapshot.StageSnapshot stage : snapshot.stages()) {
                JSONObject item = new JSONObject();
                item.put("phaseCode", stage.phaseCode());
                item.put("phaseName", stage.phaseName());
                item.put("sortOrder", stage.sortOrder());
                item.put("questionCount", stage.questionCount());
                item.put("difficultyWeight", stage.difficultyWeight());
                item.put("promptOverride", stage.promptOverride());
                item.put("phaseConfigVersion", stage.phaseConfigVersion());
                stages.add(item);
            }
            root.put("stages", stages);
            return root.toString();
        } catch (Exception exception) {
            log.error("面试模板快照序列化失败", exception);
            throw new BusinessException(ErrorCode.OBJECT_TO_JSON_ERROR);
        }
    }

    private InterviewPlanDraftDetailVO.PlanVO parsePlan(String planJson) {
        if (planJson == null || planJson.isBlank()) {
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
        try {
            JSONObject plan = JSONUtil.parseObj(planJson);
            List<InterviewPlanDraftDetailVO.StagePlanVO> stages = plan.getJSONArray("stages")
                    .stream().map(item -> {
                        JSONObject stage = (JSONObject) item;
                        return new InterviewPlanDraftDetailVO.StagePlanVO(
                                stage.getStr("phaseCode"),
                                stage.getStr("objectives"),
                                stage.getStr("questionOutline"),
                                stage.getInt("durationMinutes"));
                    }).toList();
            JSONArray suggestionsArray = plan.getJSONArray("scheduleSuggestions");
            List<InterviewPlanDraftDetailVO.ScheduleSuggestionVO> suggestions =
                    suggestionsArray == null
                            ? List.of()
                            : suggestionsArray.stream().map(item -> {
                                JSONObject suggestion = (JSONObject) item;
                                String interviewTime = suggestion.getStr("interviewTime");
                                return new InterviewPlanDraftDetailVO.ScheduleSuggestionVO(
                                        suggestion.getLong("suggestionId"),
                                        suggestion.getLong("interviewerUserId"),
                                        interviewTime == null ? null : OffsetDateTime.parse(interviewTime),
                                        suggestion.getStr("reason"));
                            }).toList();
            return new InterviewPlanDraftDetailVO.PlanVO(stages, suggestions);
        } catch (BusinessException businessException) {
            throw businessException;
        } catch (Exception exception) {
            log.error("面试编排草案计划无法解析", exception);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private void validateCurrentTemplateVersion(Long enterpriseId, InterviewPlanDraft draft) {
        // 旧数据或早期创建未带输入快照的草案无法比对版本，直接放行（不掌握模板版本即不拦截）。
        if (draft.getInputSnapshotJson() == null || draft.getInputSnapshotJson().isBlank()) {
            return;
        }
        InterviewTemplateSnapshot current = interviewStageTemplateService
                .buildTemplateSnapshotWithoutAuth(enterpriseId, draft.getTemplateId());
        if (current == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_NOT_FOUND);
        }
        Integer snapshotVersion = JSONUtil.parseObj(draft.getInputSnapshotJson()).getInt("templateVersion");
        if (snapshotVersion == null || !snapshotVersion.equals(current.templateVersion())) {
            throw new BusinessException(ErrorCode.INTERVIEW_PLAN_TEMPLATE_CHANGED);
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

    private List<Long> createScheduleFromPlan(Long enterpriseId,
                                              Long applicationId,
                                              InterviewPlanDraft draft,
                                              InterviewPlanDraftApplyReq.Plan plan,
                                              String applyIdempotencyKey) {
        // 未选中任何排期建议时按“本轮不建排期”处理，仍可完成草案认领。
        List<InterviewPlanDraftApplyReq.Suggestion> suggestions = plan.scheduleSuggestions();
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }
        // 非空建议必须与阶段一一对应：每轮一条，roundNo 从 1 连续递增。
        if (suggestions.size() != plan.stages().size()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SCHEDULE_SUGGESTION_MISMATCH);
        }
        List<Long> scheduleIds = new ArrayList<>(suggestions.size());
        for (int i = 0; i < suggestions.size(); i++) {
            InterviewPlanDraftApplyReq.Stage stage = plan.stages().get(i);
            InterviewPlanDraftApplyReq.Suggestion suggestion = suggestions.get(i);
            InterviewScheduleCreateReq createReq = new InterviewScheduleCreateReq(
                    draft.getTemplateId(),
                    (short) (i + 1),
                    suggestion.interviewerUserId(),
                    suggestion.interviewTime(),
                    stage.durationMinutes(),
                    InterviewType.TEXT);
            // 排期幂等键由应用幂等键派生，同一 apply 内每轮唯一；重试同键同计划时走草案重放分支，不会重复建排期。
            InterviewScheduleCreateVO created = interviewScheduleService.createSchedule(
                    enterpriseId, applicationId, createReq,
                    applyIdempotencyKey + ":round:" + (i + 1));
            scheduleIds.add(created.id());
        }
        return scheduleIds;
    }

    private String serializeScheduleIds(List<Long> scheduleIds) {
        try {
            return JSONUtil.toJsonStr(scheduleIds);
        } catch (Exception exception) {
            log.error("面试编排草案已应用排期 ID 序列化失败", exception);
            throw new BusinessException(ErrorCode.OBJECT_TO_JSON_ERROR);
        }
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
            JSONArray array = JSONUtil.parseArray(appliedScheduleIds);
            for (int i = 0; i < array.size(); i++) {
                ids.add(((Number) array.get(i)).longValue());
            }
            return ids;
        } catch (Exception exception) {
            log.error("面试编排草案已应用排期快照无法解析", exception);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    private String normalizePlan(InterviewPlanDraftApplyReq.Plan plan) {
        try {
            JSONObject root = new JSONObject();
            JSONArray stages = new JSONArray();
            for (InterviewPlanDraftApplyReq.Stage stage : plan.stages()) {
                JSONObject item = new JSONObject();
                item.put("phaseCode", stage.phaseCode());
                item.put("objectives", stage.objectives());
                item.put("questionOutline", stage.questionOutline());
                item.put("durationMinutes", stage.durationMinutes());
                stages.add(item);
            }
            root.put("stages", stages);
            JSONArray suggestionsArray = new JSONArray();
            if (plan.scheduleSuggestions() != null) {
                for (InterviewPlanDraftApplyReq.Suggestion suggestion : plan.scheduleSuggestions()) {
                    JSONObject item = new JSONObject();
                    item.put("suggestionId", suggestion.suggestionId());
                    item.put("interviewerUserId", suggestion.interviewerUserId());
                    item.put("interviewTime", suggestion.interviewTime() == null
                            ? null : suggestion.interviewTime().format(ISO_TIME_WITH_SECONDS));
                    item.put("reason", suggestion.reason());
                    suggestionsArray.add(item);
                }
            }
            root.put("scheduleSuggestions", suggestionsArray);
            return root.toString();
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
            return jsonEquals(JSONUtil.parse(submitted), JSONUtil.parse(stored));
        } catch (Exception exception) {
            log.error("面试编排草案应用计划对比失败", exception);
            return false;
        }
    }

    /**
     * 递归比较两棵 JSON 树；数值统一按十进制比较，容忍 int/long 序列化差异。
     */
    private boolean jsonEquals(Object left, Object right) {
        if (left == null || right == null) {
            return left == right;
        }
        if (left instanceof JSONObject leftObject && right instanceof JSONObject rightObject) {
            if (leftObject.size() != rightObject.size()) {
                return false;
            }
            for (Map.Entry<String, Object> entry : leftObject.entrySet()) {
                Object rightValue = rightObject.get(entry.getKey());
                if (rightValue == null || !jsonEquals(entry.getValue(), rightValue)) {
                    return false;
                }
            }
            return true;
        }
        if (left instanceof JSONArray leftArray && right instanceof JSONArray rightArray) {
            if (leftArray.size() != rightArray.size()) {
                return false;
            }
            for (int i = 0; i < leftArray.size(); i++) {
                if (!jsonEquals(leftArray.get(i), rightArray.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return new BigDecimal(leftNumber.toString())
                    .compareTo(new BigDecimal(rightNumber.toString())) == 0;
        }
        return Objects.equals(left, right);
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
