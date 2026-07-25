package interview.interviewcfg.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.interviewcfg.mapper.InterviewPhaseConfigMapper;
import interview.interviewcfg.mapper.InterviewStageTemplateMapper;
import interview.interviewcfg.model.entity.InterviewPhaseConfig;
import interview.interviewcfg.model.entity.InterviewStageTemplate;
import interview.interviewcfg.model.req.InterviewTemplateCreateReq;
import interview.interviewcfg.model.req.InterviewTemplateUpdateReq;
import interview.interviewcfg.model.req.PhaseConfigUpsertReq;
import interview.interviewcfg.model.vo.*;
import interview.interviewcfg.service.InterviewStageTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewStageTemplateServiceImpl
        extends ServiceImpl<InterviewStageTemplateMapper, InterviewStageTemplate>
        implements InterviewStageTemplateService {

    private static final String SORT_CREATED_AT = "createdAt";
    private static final String SORT_UPDATED_AT = "updatedAt";
    private static final String SORT_TEMPLATE_NAME = "templateName";

    private final EnterpriseValidationApi enterpriseValidationApi;
    private final InterviewPhaseConfigMapper interviewPhaseConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public InterviewTemplateCreateVO createTemplate(Long enterpriseId, InterviewTemplateCreateReq req) {
        // TODO ① 校验企业存在、当前用户属于目标企业，并确认调用方具有模板管理权限。
        // TODO ② 规范化模板名称和阶段字段，校验 phaseCode 属于支持的阶段编码，phaseCode/sortOrder 均不得重复。
        // TODO ③ 按 sortOrder 排序阶段，校验排序连续性及阶段数量上限，再序列化为 stagesSequenceJson。
        // TODO ④ 校验同一企业内模板名称等业务唯一约束，构建包含 enterpriseId、初始 version 和审计字段的实体。
        // TODO ⑤ 插入 interview_stage_templates；将唯一键冲突转换为明确业务错误，禁止静默覆盖已有模板。
        // TODO ⑥ 根据落库结果组装 id、templateName、version、createdAt；实现前需先补齐实体/表中的 version 和逻辑删除字段。
        return null;
    }

    @Override
    public IPage<InterviewTemplateListItemVO> pageTemplates(Long enterpriseId, Integer page, Integer size,
                                                             String sort, String order) {
        // 纯CRUD
        // 校验租户访问范围并限定排序字段。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        validatePage(page, size);
        if (!"asc".equalsIgnoreCase(order)
                && !"desc".equalsIgnoreCase(order)) {
            throw new BusinessException(ErrorCode.SORT_DIRECTION_INVALID);
        }
        if (!SORT_CREATED_AT.equals(sort)
                && !SORT_UPDATED_AT.equals(sort)
                && !SORT_TEMPLATE_NAME.equals(sort)) {
            throw new BusinessException(ErrorCode.SORT_FIELD_INVALID);
        }
        boolean ascending = "asc".equalsIgnoreCase(order);
        LambdaQueryWrapper<InterviewStageTemplate> query = Wrappers.lambdaQuery();
        query.eq(InterviewStageTemplate::getEnterpriseId, enterpriseId);
        switch (sort) {
            case SORT_CREATED_AT ->
                    query.orderBy(true, ascending, InterviewStageTemplate::getCreatedAt);
            case SORT_UPDATED_AT ->
                    query.orderBy(true, ascending, InterviewStageTemplate::getUpdatedAt);
            case SORT_TEMPLATE_NAME ->
                    query.orderBy(true, ascending, InterviewStageTemplate::getTemplateName);
            default -> throw new IllegalStateException("模板排序字段校验失效");
        }
        query.orderBy(true, ascending, InterviewStageTemplate::getId);

        // 分页查询模板并转换为列表响应。
        IPage<InterviewStageTemplate> entityPage =
                page(new Page<>(page, size), query);
        List<InterviewTemplateListItemVO> records = entityPage.getRecords().stream()
                .map(template -> new InterviewTemplateListItemVO(
                        template.getId(),
                        template.getTemplateName(),
                        countStages(template),
                        template.getVersion(),
                        template.getCreatedAt(),
                        template.getUpdatedAt()
                ))
                .toList();
        Page<InterviewTemplateListItemVO> result =
                new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public InterviewTemplateDetailVO getTemplateDetail(Long enterpriseId, Long templateId) {
        // 纯CRUD
        // 校验租户范围并查询目标模板。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        InterviewStageTemplate template = lambdaQuery()
                .eq(InterviewStageTemplate::getId, templateId)
                .eq(InterviewStageTemplate::getEnterpriseId, enterpriseId)
                .one();
        if (template == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_NOT_FOUND);
        }

        // 解析阶段顺序并批量查询组卷配置。
        List<StageVO> stages = parseStages(template);
        List<InterviewPhaseConfig> configs = interviewPhaseConfigMapper.selectList(
                Wrappers.<InterviewPhaseConfig>lambdaQuery()
                        .eq(InterviewPhaseConfig::getTemplateId, templateId)
        );
        List<PhaseConfigVO> phaseConfigs = sortAndMapConfigs(stages, configs, templateId);
        return new InterviewTemplateDetailVO(
                template.getId(),
                template.getTemplateName(),
                stages,
                phaseConfigs,
                template.getVersion(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public InterviewTemplateUpdateVO updateTemplate(Long enterpriseId, Long templateId, InterviewTemplateUpdateReq req) {
        // TODO ① 按 templateId + enterpriseId 查询未删除模板，校验租户归属并取得当前 version。
        // TODO ② 至少要求 templateName 或 stages 存在；名称变更时规范化并校验企业内名称唯一性。
        // TODO ③ stages 非空时执行创建接口相同的编码、重复项、排序和数量校验，并全量序列化替换阶段 JSON。
        // TODO ④ 检查被移除阶段是否仍有 phaseConfig 或已被排期引用，按业务规则拒绝删除或同步清理孤立配置。
        // TODO ⑤ 使用 id + enterpriseId + version=expectedVersion 条件更新允许变更字段，同时执行 version=version+1 和审计填充。
        // TODO ⑥ 更新零行时重新判断模板不存在还是版本冲突；成功后返回新 version 和 updatedAt。
        return null;
    }

    @Override
    @Transactional
    public InterviewTemplateDeleteVO deleteTemplate(Long enterpriseId, Long templateId, Integer expectedVersion) {
        // TODO ① 查询目标企业下未删除模板并校验 expectedVersion；当前实体/表需先补齐 version 与 @TableLogic 字段。
        // TODO ② 检查是否存在尚未结束的 interview_schedule 引用该模板，存在时禁止删除以保留会话配置一致性。
        // TODO ③ 使用 id + enterpriseId + version 条件执行逻辑删除，并递增 version、写入 updatedBy/updatedAt/traceId。
        // TODO ④ 更新零行时区分不存在、已删除和并发版本冲突，保证重复删除具有明确响应语义。
        // TODO ⑤ 保留历史排期所需的模板快照或配置引用，成功后返回统一删除结果。
        return null;
    }

    @Override
    @Transactional
    public PhaseConfigVO upsertPhaseConfig(Long enterpriseId, Long templateId, String phaseCode, PhaseConfigUpsertReq req) {
        // TODO ① 查询 enterpriseId 下未删除模板，解析 stagesSequenceJson 并确认 phaseCode 确实属于模板阶段。
        // TODO ② 复核 questionCount、difficultyWeight 的业务范围；promptOverride 为空表示清除旧自定义提示词。
        // TODO ③ 按 templateId + phaseCode 查询已有配置，存在则使用实体完整替换，不存在则构建新实体插入。
        // TODO ④ 依赖唯一约束保证同一模板同一阶段只有一条配置，并将并发插入冲突转换为重试或业务冲突。
        // TODO ⑤ 重新读取落库结果并映射 PhaseConfigVO，返回服务端最终 updatedAt。
        return null;
    }

    @Override
    public List<PhaseConfigVO> listPhaseConfigs(Long enterpriseId, Long templateId) {
        // 纯CRUD
        // 校验模板归属，禁止跨企业读取配置。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        InterviewStageTemplate template = lambdaQuery()
                .eq(InterviewStageTemplate::getId, templateId)
                .eq(InterviewStageTemplate::getEnterpriseId, enterpriseId)
                .one();
        if (template == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_NOT_FOUND);
        }

        // 按模板阶段顺序返回已经配置的组卷策略。
        List<StageVO> stages = parseStages(template);
        List<InterviewPhaseConfig> configs = interviewPhaseConfigMapper.selectList(
                Wrappers.<InterviewPhaseConfig>lambdaQuery()
                        .eq(InterviewPhaseConfig::getTemplateId, templateId)
        );
        return sortAndMapConfigs(stages, configs, templateId);
    }

    private int countStages(InterviewStageTemplate template) {
        try {
            return parseStages(template).size();
        } catch (BusinessException exception) {
            log.error("面试模板阶段 JSON 无法解析。templateId={}", template.getId(), exception);
            return 0;
        }
    }

    private List<StageVO> parseStages(InterviewStageTemplate template) {
        if (StrUtil.isBlank(template.getStagesSequenceJson())) {
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
        try {
            StageVO[] stages = objectMapper.readValue(
                    template.getStagesSequenceJson(), StageVO[].class);
            return new ArrayList<>(List.of(stages)).stream()
                    .sorted(Comparator.comparing(
                            StageVO::sortOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.JSON_TO_OBJECT_ERROR, "面试模板阶段配置无法解析");
        }
    }

    private List<PhaseConfigVO> sortAndMapConfigs(List<StageVO> stages,
                                                   List<InterviewPhaseConfig> configs,
                                                   Long templateId) {
        Map<String, Integer> phaseOrder = new HashMap<>();
        for (int index = 0; index < stages.size(); index++) {
            phaseOrder.put(stages.get(index).phaseCode(), index);
        }
        return configs.stream()
                .sorted(Comparator
                        .comparingInt((InterviewPhaseConfig config) ->
                                phaseOrder.getOrDefault(config.getPhaseCode(), Integer.MAX_VALUE))
                        .thenComparing(InterviewPhaseConfig::getPhaseCode))
                .peek(config -> {
                    if (!phaseOrder.containsKey(config.getPhaseCode())) {
                        log.warn("阶段配置不属于模板阶段。templateId={}, phaseCode={}",
                                templateId, config.getPhaseCode());
                    }
                })
                .map(config -> new PhaseConfigVO(
                        config.getId(),
                        config.getPhaseCode(),
                        config.getQuestionCount(),
                        config.getDifficultyWeight(),
                        config.getPromptOverride(),
                        config.getUpdatedAt()
                ))
                .toList();
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }
}
