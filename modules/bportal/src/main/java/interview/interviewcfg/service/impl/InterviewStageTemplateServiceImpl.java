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
import interview.interviewcfg.model.bo.InterviewTemplateSnapshot;
import interview.interviewcfg.model.entity.InterviewPhaseConfig;
import interview.interviewcfg.model.entity.InterviewStageTemplate;
import interview.interviewcfg.model.req.InterviewTemplateCreateReq;
import interview.interviewcfg.model.req.InterviewTemplateUpdateReq;
import interview.interviewcfg.model.req.PhaseConfigUpsertReq;
import interview.interviewcfg.model.vo.*;
import interview.interviewcfg.service.InterviewStageTemplateService;
import interview.interviewcfg.service.support.InterviewTemplateStageRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        // 校验企业归属并规范化模板阶段。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        String templateName = StrUtil.trim(req.templateName());
        List<StageVO> stages = InterviewTemplateStageRules.normalize(req.stages());
        InterviewStageTemplate template = InterviewStageTemplate.builder()
                .enterpriseId(enterpriseId)
                .templateName(templateName)
                .stagesSequenceJson(writeStages(stages))
                .version(0)
                .build();
        try {
            baseMapper.insert(template);
            // 模板创建后立即补齐每个阶段的默认组卷配置。
            for (StageVO stage : stages) {
                interviewPhaseConfigMapper.insert(defaultPhaseConfig(template.getId(), stage.phaseCode()));
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_ALREADY_EXISTS);
        }
        return new InterviewTemplateCreateVO(
                template.getId(), template.getTemplateName(), template.getVersion(), template.getCreatedAt());
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
        // 使用模板版本阻止多个管理端基于旧数据相互覆盖。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        InterviewStageTemplate current = lambdaQuery()
                .eq(InterviewStageTemplate::getId, templateId)
                .eq(InterviewStageTemplate::getEnterpriseId, enterpriseId)
                .one();
        if (current == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_NOT_FOUND);
        }
        if (!current.getVersion().equals(req.expectedVersion())) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_VERSION_CONFLICT);
        }
        if (req.templateName() == null && req.stages() == null) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }

        String templateName = req.templateName() == null ? null : StrUtil.trim(req.templateName());
        if (templateName != null && templateName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }
        List<StageVO> stages = req.stages() == null
                ? null : InterviewTemplateStageRules.normalize(req.stages());
        InterviewStageTemplate update = InterviewStageTemplate.builder()
                .id(templateId)
                .templateName(templateName)
                .stagesSequenceJson(stages == null ? null : writeStages(stages))
                .version(req.expectedVersion())
                .build();
        try {
            if (baseMapper.updateById(update) != 1) {
                throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_VERSION_CONFLICT);
            }
            if (stages != null) {
                syncPhaseConfigs(templateId, stages);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_ALREADY_EXISTS);
        }
        return new InterviewTemplateUpdateVO(
                templateId,
                templateName == null ? current.getTemplateName() : templateName,
                req.expectedVersion() + 1,
                update.getUpdatedAt());
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
        // 阶段配置只能绑定模板中已经声明的阶段编码。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        InterviewStageTemplate template = lambdaQuery()
                .eq(InterviewStageTemplate::getId, templateId)
                .eq(InterviewStageTemplate::getEnterpriseId, enterpriseId)
                .one();
        if (template == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_NOT_FOUND);
        }
        String normalizedPhaseCode = StrUtil.trim(phaseCode).toUpperCase();
        boolean phaseExists = parseStages(template).stream()
                .anyMatch(stage -> stage.phaseCode().equals(normalizedPhaseCode));
        if (!phaseExists) {
            throw new BusinessException(ErrorCode.INTERVIEW_PHASE_CONFIG_NOT_FOUND);
        }

        InterviewPhaseConfig current = interviewPhaseConfigMapper.selectOne(
                Wrappers.<InterviewPhaseConfig>lambdaQuery()
                        .eq(InterviewPhaseConfig::getTemplateId, templateId)
                        .eq(InterviewPhaseConfig::getPhaseCode, normalizedPhaseCode));
        InterviewPhaseConfig config = InterviewPhaseConfig.builder()
                .id(current == null ? null : current.getId())
                .templateId(templateId)
                .phaseCode(normalizedPhaseCode)
                .questionCount(req.questionCount())
                .difficultyWeight(req.difficultyWeight())
                .promptOverride(req.promptOverride())
                .version(current == null ? 0 : current.getVersion())
                .build();
        try {
            if (current == null) {
                interviewPhaseConfigMapper.insert(config);
            } else if (interviewPhaseConfigMapper.updateById(config) != 1) {
                throw new BusinessException(ErrorCode.INTERVIEW_PHASE_CONFIG_VERSION_CONFLICT);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.INTERVIEW_PHASE_CONFIG_VERSION_CONFLICT);
        }
        return toPhaseConfigVO(config);
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

    @Override
    public InterviewTemplateSnapshot buildTemplateSnapshot(Long enterpriseId, Long templateId) {
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        InterviewStageTemplate template = lambdaQuery()
                .eq(InterviewStageTemplate::getId, templateId)
                .eq(InterviewStageTemplate::getEnterpriseId, enterpriseId)
                .one();
        if (template == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_NOT_FOUND);
        }

        Map<String, InterviewPhaseConfig> configs = interviewPhaseConfigMapper.selectList(
                        Wrappers.<InterviewPhaseConfig>lambdaQuery()
                                .eq(InterviewPhaseConfig::getTemplateId, templateId))
                .stream()
                .collect(Collectors.toMap(InterviewPhaseConfig::getPhaseCode, Function.identity()));
        List<InterviewTemplateSnapshot.StageSnapshot> stages = parseStages(template).stream()
                .map(stage -> toStageSnapshot(templateId, stage, configs.get(stage.phaseCode())))
                .toList();
        return new InterviewTemplateSnapshot(
                template.getId(), template.getVersion(), template.getTemplateName(), stages);
    }

    private String writeStages(List<StageVO> stages) {
        try {
            return objectMapper.writeValueAsString(stages);
        } catch (Exception exception) {
            log.error("面试模板阶段序列化失败", exception);
            throw new BusinessException(ErrorCode.OBJECT_TO_JSON_ERROR);
        }
    }

    private InterviewPhaseConfig defaultPhaseConfig(Long templateId, String phaseCode) {
        return InterviewPhaseConfig.builder()
                .templateId(templateId)
                .phaseCode(phaseCode)
                .questionCount(3)
                .difficultyWeight(0.5)
                .version(0)
                .build();
    }

    private void syncPhaseConfigs(Long templateId, List<StageVO> stages) {
        Map<String, InterviewPhaseConfig> current = interviewPhaseConfigMapper.selectList(
                        Wrappers.<InterviewPhaseConfig>lambdaQuery()
                                .eq(InterviewPhaseConfig::getTemplateId, templateId))
                .stream()
                .collect(Collectors.toMap(InterviewPhaseConfig::getPhaseCode, Function.identity()));
        List<String> retainedCodes = stages.stream().map(StageVO::phaseCode).toList();
        interviewPhaseConfigMapper.delete(
                Wrappers.<InterviewPhaseConfig>lambdaQuery()
                        .eq(InterviewPhaseConfig::getTemplateId, templateId)
                        .notIn(!retainedCodes.isEmpty(), InterviewPhaseConfig::getPhaseCode, retainedCodes));
        for (String phaseCode : retainedCodes) {
            if (!current.containsKey(phaseCode)) {
                interviewPhaseConfigMapper.insert(defaultPhaseConfig(templateId, phaseCode));
            }
        }
    }

    private InterviewTemplateSnapshot.StageSnapshot toStageSnapshot(
            Long templateId, StageVO stage, InterviewPhaseConfig config) {
        if (config == null) {
            log.error("面试模板阶段缺少组卷配置。templateId={}, phaseCode={}", templateId, stage.phaseCode());
            throw new BusinessException(ErrorCode.INTERVIEW_PHASE_CONFIG_NOT_FOUND);
        }
        return new InterviewTemplateSnapshot.StageSnapshot(
                stage.phaseCode(), stage.phaseName(), stage.sortOrder(),
                config.getQuestionCount(), config.getDifficultyWeight(), config.getPromptOverride(), config.getVersion());
    }

    private PhaseConfigVO toPhaseConfigVO(InterviewPhaseConfig config) {
        return new PhaseConfigVO(
                config.getId(), config.getPhaseCode(), config.getQuestionCount(),
                config.getDifficultyWeight(), config.getPromptOverride(), config.getUpdatedAt());
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
