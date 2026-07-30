package interview.ai.config.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.ai.config.event.LLMSceneChangeEvent;
import interview.ai.config.mapper.AiGlobalRouteMapper;
import interview.ai.config.mapper.LlmProviderConfigMapper;
import interview.ai.config.mapper.LlmSceneConfigMapper;
import interview.ai.config.model.entity.AiGlobalRoute;
import interview.ai.config.model.entity.LlmProviderConfig;
import interview.ai.config.model.entity.LlmSceneConfig;
import interview.common.enums.AiSceneCode;
import interview.ai.config.model.req.LlmSceneQueryReq;
import interview.ai.config.model.req.LlmSceneStatusReq;
import interview.ai.config.model.req.LlmSceneUpdateReq;
import interview.ai.config.model.vo.LlmScenePageVO;
import interview.ai.config.model.vo.LlmSceneStatusVO;
import interview.ai.config.model.vo.LlmSceneVO;
import interview.ai.config.service.LlmSceneConfigService;
import interview.common.enums.AiModelType;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LlmSceneConfigServiceImpl extends ServiceImpl<LlmSceneConfigMapper, LlmSceneConfig>
        implements LlmSceneConfigService {

    private static final BigDecimal MIN_TEMPERATURE = BigDecimal.ZERO;
    private static final BigDecimal MAX_TEMPERATURE = BigDecimal.valueOf(2);
    private static final BigDecimal MIN_TOP_P = BigDecimal.ZERO;
    private static final BigDecimal MAX_TOP_P = BigDecimal.ONE;
    private final LlmSceneConfigMapper llmSceneConfigMapper;
    private final AiGlobalRouteMapper aiGlobalRouteMapper;
    private final LlmProviderConfigMapper llmProviderConfigMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public LlmScenePageVO pageScenes(LlmSceneQueryReq req) {
        // 组合模型类型、启停状态和关键字过滤，or 条件限制在同一分组内。
        String keyword = StrUtil.isBlank(req.getKeyword()) ? null : req.getKeyword().trim();
        LambdaQueryWrapper<LlmSceneConfig> wrapper = new LambdaQueryWrapper<LlmSceneConfig>()
                .eq(req.getModelType() != null, LlmSceneConfig::getModelType, req.getModelType())
                .eq(req.getEnabled() != null, LlmSceneConfig::getEnabled, req.getEnabled())
                .and(StrUtil.isNotBlank(keyword), query -> query
                        .like(LlmSceneConfig::getSceneCode, keyword)
                        .or()
                        .like(LlmSceneConfig::getProviderId, keyword)
                        .or()
                        .like(LlmSceneConfig::getPromptVersion, keyword));
        applySorting(wrapper, req.getSort(), req.getOrder());

        // 执行分页查询并转换统一分页响应。
        Page<LlmSceneConfig> rawPage = llmSceneConfigMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()),
                wrapper
        );
        List<LlmSceneVO> records = rawPage.getRecords().stream()
                .map(this::toSceneVO)
                .toList();
        return new LlmScenePageVO(
                rawPage.getCurrent(),
                Math.toIntExact(rawPage.getSize()),
                rawPage.getTotal(),
                rawPage.getPages(),
                records
        );
    }

    @Override
    @Cacheable(value = "llmScene",key = "#sceneCode")
    public LlmSceneVO getScene(AiSceneCode sceneCode) {
        // 按场景编码查询完整配置。
        return toSceneVO(requireScene(sceneCode));
    }

    @Override
    @Transactional
    public LlmSceneVO updateScene(AiSceneCode sceneCode, LlmSceneUpdateReq req) {
        String providerId = normalizeOptionalProviderId(req.getProviderId());
        String promptVersion = normalizePromptVersion(req.getPromptVersion());
        String extraOptions = normalizeExtraOptions(req.getExtraOptions());
        validateSceneParameters(req);

        // 使用默认路由时先读取不可变模型类型，再按默认路由、场景、Provider 的顺序加锁。
        LlmSceneConfig snapshot = requireScene(sceneCode);
        AiGlobalRoute route = providerId == null
                ? requireLockedGlobalRoute(snapshot.getModelType())
                : null;
        LlmSceneConfig current = requireLockedScene(sceneCode);
        requireExpectedVersion(current, req.getExpectedVersion());

        // 锁定并校验场景最终使用的 Provider。
        String resolvedProviderId = providerId != null
                ? providerId
                : resolveDefaultProviderId(route);
        requireLockedEnabledProvider(resolvedProviderId, current.getModelType());

        // 全量更新可编辑参数，XML 负责 CAS、版本递增和审计字段写入。
        LlmSceneConfig update = auditedScene(sceneCode);
        update.setProviderId(providerId);
        update.setTemperature(req.getTemperature());
        update.setTopP(req.getTopP());
        update.setMaxInputTokens(req.getMaxInputTokens());
        update.setMaxOutputTokens(req.getMaxOutputTokens());
        update.setTimeoutSeconds(req.getTimeoutSeconds());
        update.setPromptVersion(promptVersion);
        update.setExtraOptions(extraOptions);
        int affected = llmSceneConfigMapper.updateSceneByVersion(update, req.getExpectedVersion());
        if (affected != 1) {
            throw new BusinessException(ErrorCode.AI_SCENE_VERSION_CONFLICT);
        }

        //   路由缓存完成后，在事务提交后发布场景配置变更事件。
        eventPublisher.publishEvent(new LLMSceneChangeEvent(sceneCode));
        return toSceneVO(requireScene(sceneCode));
    }

    @Override
    @Transactional
    public LlmSceneStatusVO updateSceneStatus(AiSceneCode sceneCode, LlmSceneStatusReq req) {

        // 先读取路由类型；仅启用全局路由场景时需要提前锁定全局设置。
        LlmSceneConfig snapshot = requireScene(sceneCode);
        requireExpectedVersion(snapshot, req.getExpectedVersion());
        boolean needsGlobalSetting = Boolean.TRUE.equals(req.getEnabled())
                && StrUtil.isBlank(snapshot.getProviderId());
        AiGlobalRoute route = needsGlobalSetting
                ? requireLockedGlobalRoute(snapshot.getModelType())
                : null;

        LlmSceneConfig current = requireLockedScene(sceneCode);
        requireExpectedVersion(current, req.getExpectedVersion());

        // 启用前确认最终路由可用；停用不依赖 Provider 当前状态。
        if (Boolean.TRUE.equals(req.getEnabled())) {
            String resolvedProviderId = StrUtil.isNotBlank(current.getProviderId())
                    ? current.getProviderId()
                    : resolveDefaultProviderId(route);
            requireLockedEnabledProvider(resolvedProviderId, current.getModelType());
        }
        if (Objects.equals(current.getEnabled(), req.getEnabled())) {
            return toSceneStatusVO(current);
        }

        LlmSceneConfig update = auditedScene(sceneCode);
        update.setEnabled(req.getEnabled());
        int affected = llmSceneConfigMapper.updateSceneStatusByVersion(update, req.getExpectedVersion());
        if (affected != 1) {
            throw new BusinessException(ErrorCode.AI_SCENE_VERSION_CONFLICT);
        }

        LlmSceneConfig latest = requireScene(sceneCode);
        //   路由缓存完成后，在事务提交后发布场景启停事件。
        eventPublisher.publishEvent(new LLMSceneChangeEvent(sceneCode));
        return toSceneStatusVO(latest);
    }

    private LlmSceneVO toSceneVO(LlmSceneConfig scene) {
        return new LlmSceneVO(
                scene.getSceneCode(),
                scene.getModelType(),
                scene.getProviderId(),
                scene.getTemperature(),
                scene.getTopP(),
                scene.getMaxInputTokens(),
                scene.getMaxOutputTokens(),
                scene.getTimeoutSeconds(),
                scene.getPromptVersion(),
                scene.getExtraOptions(),
                scene.getEnabled(),
                scene.getVersion(),
                scene.getUpdatedAt()
        );
    }

    private LlmSceneStatusVO toSceneStatusVO(LlmSceneConfig scene) {
        return new LlmSceneStatusVO(
                scene.getSceneCode(),
                scene.getEnabled(),
                scene.getVersion(),
                scene.getUpdatedAt()
        );
    }

    /**
     * 应用分页查询的白名单排序规则。
     */
    private void applySorting(LambdaQueryWrapper<LlmSceneConfig> wrapper,
                              String sort,
                              String order) {
        boolean ascending = "asc".equalsIgnoreCase(order);
        String sortField = StrUtil.blankToDefault(sort, "createdAt");
        switch (sortField) {
            case "sceneCode" -> wrapper.orderBy(true, ascending, LlmSceneConfig::getSceneCode);
            case "modelType" -> wrapper.orderBy(true, ascending, LlmSceneConfig::getModelType);
            case "providerId" -> wrapper.orderBy(true, ascending, LlmSceneConfig::getProviderId);
            case "enabled" -> wrapper.orderBy(true, ascending, LlmSceneConfig::getEnabled);
            case "updatedAt" -> wrapper.orderBy(true, ascending, LlmSceneConfig::getUpdatedAt);
            default -> wrapper.orderBy(true, ascending, LlmSceneConfig::getCreatedAt);
        }
        if (!"sceneCode".equals(sortField)) {
            wrapper.orderByAsc(LlmSceneConfig::getSceneCode);
        }
    }

    /**
     * 查询场景，不存在时抛出统一业务异常。
     */
    private LlmSceneConfig requireScene(AiSceneCode sceneCode) {
        LlmSceneConfig scene = llmSceneConfigMapper.selectById(sceneCode);
        if (scene == null) {
            throw new BusinessException(ErrorCode.AI_SCENE_NOT_FOUND);
        }
        return scene;
    }

    /**
     * 锁定场景，不存在时抛出统一业务异常。
     */
    private LlmSceneConfig requireLockedScene(AiSceneCode sceneCode) {
        LlmSceneConfig scene = llmSceneConfigMapper.lockBySceneCode(sceneCode.name());
        if (scene == null) {
            throw new BusinessException(ErrorCode.AI_SCENE_NOT_FOUND);
        }
        return scene;
    }

    /**
     * 锁定指定模型类型的全局默认路由。
     */
    private AiGlobalRoute requireLockedGlobalRoute(AiModelType modelType) {
        AiGlobalRoute route = aiGlobalRouteMapper.lockByModelType(modelType);
        if (route == null) {
            throw new BusinessException(ErrorCode.AI_GLOBAL_ROUTE_NOT_FOUND);
        }
        return route;
    }

    /**
     * 从已锁定的全局路由解析默认 Provider。
     */
    private String resolveDefaultProviderId(AiGlobalRoute route) {
        String providerId = route.getProviderId();
        if (StrUtil.isBlank(providerId)) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_NOT_FOUND);
        }
        return providerId;
    }

    /**
     * 锁定并校验 Provider 可用性。
     */
    private LlmProviderConfig requireLockedEnabledProvider(String providerId,
                                                           AiModelType modelType) {
        LlmProviderConfig provider = llmProviderConfigMapper.lockByIdIncludingDeleted(providerId);
        if (provider == null || Boolean.TRUE.equals(provider.getIsDeleted())) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(provider.getEnabled())) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_DISABLED);
        }
        if (provider.getModelType() != modelType) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_TYPE_MISMATCH);
        }
        return provider;
    }

    /**
     * 校验场景版本。
     */
    private void requireExpectedVersion(LlmSceneConfig scene, Integer expectedVersion) {
        if (!Objects.equals(scene.getVersion(), expectedVersion)) {
            throw new BusinessException(ErrorCode.AI_SCENE_VERSION_CONFLICT);
        }
    }

    /**
     * 校验平台级场景参数边界。
     */
    private void validateSceneParameters(LlmSceneUpdateReq req) {
        if (req.getExpectedVersion() == null || req.getExpectedVersion() < 0
                || outsideClosedRange(req.getTemperature(), MIN_TEMPERATURE, MAX_TEMPERATURE)
                || outsideOpenClosedRange(req.getTopP(), MIN_TOP_P, MAX_TOP_P)
                || outsideRange(req.getMaxInputTokens(), 256, 1_000_000)
                || outsideRange(req.getMaxOutputTokens(), 1, 32_768)
                || outsideRange(req.getTimeoutSeconds(), 5, 180)) {
            throw new BusinessException(ErrorCode.AI_SCENE_PARAM_UNSUPPORTED);
        }
    }

    private boolean outsideClosedRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value != null && (value.compareTo(min) < 0 || value.compareTo(max) > 0);
    }

    private boolean outsideOpenClosedRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value != null && (value.compareTo(min) <= 0 || value.compareTo(max) > 0);
    }

    private boolean outsideRange(Integer value, int min, int max) {
        return value == null || value < min || value > max;
    }

    /**
     * 规范化可选 Provider ID。
     */
    private String normalizeOptionalProviderId(String providerId) {
        return StrUtil.isBlank(providerId) ? null : providerId.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化提示词版本。
     */
    private String normalizePromptVersion(String promptVersion) {
        if (StrUtil.isBlank(promptVersion) || promptVersion.trim().length() > 64) {
            throw new BusinessException(ErrorCode.AI_SCENE_PARAM_UNSUPPORTED);
        }
        return promptVersion.trim();
    }

    /**
     * 校验并规范化供应商特有参数。
     */
    private String normalizeExtraOptions(String extraOptions) {
        try {
            JSON json = JSONUtil.parse(extraOptions);
            if (!(json instanceof JSONObject options)) {
                throw new BusinessException(ErrorCode.AI_SCENE_PARAM_UNSUPPORTED);
            }
            // ponytail: 供应商参数白名单尚未落地，当前仅允许空对象；有正式适配器选项时再开放对应字段。
            if (!options.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_SCENE_PARAM_UNSUPPORTED);
            }
            return options.toString();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AI_SCENE_PARAM_UNSUPPORTED);
        }
    }

    private LlmSceneConfig auditedScene(AiSceneCode sceneCode) {
        return LlmSceneConfig.builder()
                .sceneCode(sceneCode)
                .updatedBy(AuthContext.getUserIdOrNull())
                .traceId(TraceUtil.getTraceId())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
