package interview.ai.config.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.ai.config.event.LLMGlobalRouteChangeEvent;
import interview.ai.config.mapper.AiGlobalRouteMapper;
import interview.ai.config.mapper.LlmProviderConfigMapper;
import interview.ai.config.model.entity.AiGlobalRoute;
import interview.ai.config.model.entity.LlmProviderConfig;
import interview.ai.config.model.req.AiGlobalRouteUpdateReq;
import interview.ai.config.model.vo.AiGlobalRouteListVO;
import interview.ai.config.model.vo.AiGlobalRouteVO;
import interview.ai.config.service.AiGlobalRouteService;
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

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiGlobalRouteServiceImpl extends ServiceImpl<AiGlobalRouteMapper, AiGlobalRoute>
        implements AiGlobalRouteService {

    private static final Pattern PROVIDER_ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");
    private final LlmProviderConfigMapper llmProviderConfigMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Cacheable(value = "llmGlobalRoute", key = "'ListGlobalRoute'")
    public AiGlobalRouteListVO listRoutes() {
        // 纯 CRUD：全局路由是固定的小表，按模型类型稳定返回。
        return new AiGlobalRouteListVO(
                baseMapper.selectList(
                        new LambdaQueryWrapper<AiGlobalRoute>()
                        .select(
                                AiGlobalRoute::getModelType,
                                AiGlobalRoute::getProviderId,
                                AiGlobalRoute::getVersion,
                                AiGlobalRoute::getUpdatedAt
                        )
                        .orderByAsc(AiGlobalRoute::getModelType)
                )
                        .stream()
                        .map(this::toRouteVO)
                        .toList()
        );
    }

    @Override
    @Transactional
    public AiGlobalRouteVO updateRoute(AiModelType modelType, AiGlobalRouteUpdateReq req) {
        String providerId = normalizeProviderId(req.getProviderId());

        // 先锁定模型类型对应的固定路由槽位，再校验版本。
        AiGlobalRoute current = baseMapper.lockByModelType(modelType);
        if (current == null) {
            throw new BusinessException(ErrorCode.AI_GLOBAL_ROUTE_NOT_FOUND);
        }
        if (!Objects.equals(current.getVersion(), req.getExpectedVersion())) {
            throw new BusinessException(ErrorCode.AI_GLOBAL_ROUTE_VERSION_CONFLICT);
        }

        // 默认路由只能绑定同类型且已启用的有效 Provider。
        LlmProviderConfig provider = llmProviderConfigMapper.lockByIdIncludingDeleted(providerId);
        requireAvailableProvider(provider, modelType);

        AiGlobalRoute update = AiGlobalRoute.builder()
                .modelType(modelType)
                .providerId(providerId)
                .updatedBy(AuthContext.getRequiredUserId())
                .traceId(TraceUtil.getTraceId())
                .updatedAt(OffsetDateTime.now())
                .build();
        int affected = baseMapper.updateRouteByVersion(update, req.getExpectedVersion());
        if (affected != 1) {
            throw new BusinessException(ErrorCode.AI_GLOBAL_ROUTE_VERSION_CONFLICT);
        }

        //路由缓存完成后，在事务提交后发布全局路由变更事件。
        eventPublisher.publishEvent(new LLMGlobalRouteChangeEvent(this));
        AiGlobalRoute latest = baseMapper.selectById(modelType);
        if (latest == null) {
            throw new BusinessException(ErrorCode.AI_GLOBAL_ROUTE_NOT_FOUND);
        }
        return toRouteVO(latest);
    }

    /**
     * 校验 Provider 存在、启用且模型类型与路由一致。
     */
    private void requireAvailableProvider(LlmProviderConfig provider, AiModelType modelType) {
        if (provider == null || Boolean.TRUE.equals(provider.getIsDeleted())) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(provider.getEnabled())) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_DISABLED);
        }
        if (provider.getModelType() != modelType) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_TYPE_MISMATCH);
        }
    }

    /**
     * 规范化并校验 Provider ID。
     */
    private String normalizeProviderId(String providerId) {
        if (StrUtil.isBlank(providerId)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "Provider ID不能为空");
        }
        String normalizedProviderId = providerId.trim().toLowerCase(Locale.ROOT);
        if (!PROVIDER_ID_PATTERN.matcher(normalizedProviderId).matches()) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "Provider ID格式不合法");
        }
        return normalizedProviderId;
    }

    private AiGlobalRouteVO toRouteVO(AiGlobalRoute route) {
        return new AiGlobalRouteVO(
                route.getModelType(),
                route.getProviderId(),
                route.getVersion(),
                route.getUpdatedAt()
        );
    }
}
