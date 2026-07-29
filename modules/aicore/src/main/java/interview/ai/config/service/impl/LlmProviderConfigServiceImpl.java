package interview.ai.config.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.ai.config.mapper.AiGlobalRouteMapper;
import interview.ai.config.mapper.LlmProviderConfigMapper;
import interview.ai.config.mapper.LlmSceneConfigMapper;
import interview.ai.config.model.entity.AiGlobalRoute;
import interview.ai.config.model.entity.LlmProviderConfig;
import interview.ai.config.model.entity.LlmSceneConfig;
import interview.ai.config.model.req.LlmProviderCreateReq;
import interview.ai.config.model.req.LlmProviderQueryReq;
import interview.ai.config.model.req.LlmProviderStatusReq;
import interview.ai.config.model.req.LlmProviderUpdateReq;
import interview.ai.config.model.vo.LlmProviderDeleteVO;
import interview.ai.config.model.vo.LlmProviderPageVO;
import interview.ai.config.model.vo.LlmProviderStatusVO;
import interview.ai.config.model.vo.LlmProviderTestConnectionVO;
import interview.ai.config.model.vo.LlmProviderVO;
import interview.ai.config.service.LlmProviderConfigService;
import interview.common.enums.AiModelType;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.security.SecretCipher;
import interview.common.security.SecretCipherException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LlmProviderConfigServiceImpl extends ServiceImpl<LlmProviderConfigMapper, LlmProviderConfig>
        implements LlmProviderConfigService {

    private static final String MASKED_API_KEY = "******";
    private static final Pattern PROVIDER_ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(8);

    private final LlmProviderConfigMapper llmProviderConfigMapper;
    private final AiGlobalRouteMapper aiGlobalRouteMapper;
    private final LlmSceneConfigMapper llmSceneConfigMapper;
    private final SecretCipher secretCipher;

    @Override
    @Transactional
    public LlmProviderVO createProvider(LlmProviderCreateReq req) {
        // 规范化并校验外部配置，避免非法地址和不稳定 ID 进入路由配置。
        String providerId = verifyProviderId(req.getProviderId());
        String baseUrl = verifyBaseUrl(req.getBaseUrl());
        String model = verifyModel(req.getModel());
        AiModelType modelType = req.getModelType();
        String plainApiKey = verifyApiKey(req.getApiKey());

        // 同名有效配置禁止重复创建；逻辑删除配置允许用本次的新密钥和模型恢复。
        LlmProviderConfig existing = llmProviderConfigMapper.selectByIdIncludingDeleted(providerId);
        if (existing != null && !Boolean.TRUE.equals(existing.getIsDeleted())) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ALREADY_EXISTS);
        }

        // 明文密钥只在加密调用的最小作用域内使用，数据库仅保存 AES-GCM 密文信封。
        String encryptedApiKey = secretCipher.encrypt(plainApiKey);
        if (existing == null) {
            insertProvider(providerId, baseUrl, model, modelType, encryptedApiKey, req.getEnabled());
        } else {
            restoreProvider(existing, baseUrl, model, modelType, encryptedApiKey, req.getEnabled());
        }

        // 重新查询数据库值，响应始终使用固定掩码且不暴露密文。
        return toProviderVO(requireActiveProvider(providerId));
    }

    @Override
    public LlmProviderPageVO pageProviders(LlmProviderQueryReq req) {
        // 组合启停状态、关键字和白名单排序条件。
        String keyword = StrUtil.isBlank(req.getKeyword()) ? null : req.getKeyword().trim();
        LambdaQueryWrapper<LlmProviderConfig> wrapper = new LambdaQueryWrapper<LlmProviderConfig>()
                .select(
                        LlmProviderConfig::getId,
                        LlmProviderConfig::getBaseUrl,
                        LlmProviderConfig::getModel,
                        LlmProviderConfig::getModelType,
                        LlmProviderConfig::getEnabled,
                        LlmProviderConfig::getVersion,
                        LlmProviderConfig::getCreatedAt,
                        LlmProviderConfig::getUpdatedAt
                )
                .eq(req.getEnabled() != null, LlmProviderConfig::getEnabled, req.getEnabled())
                .eq(req.getModelType() != null, LlmProviderConfig::getModelType, req.getModelType())
                .and(StrUtil.isNotBlank(keyword), query -> query
                        .like(LlmProviderConfig::getId, keyword)
                        .or()
                        .like(LlmProviderConfig::getBaseUrl, keyword)
                        .or()
                        .like(LlmProviderConfig::getModel, keyword));
        applySorting(wrapper, req.getSort(), req.getOrder());

        // 执行分页查询，并将密钥字段转换为固定掩码。
        Page<LlmProviderConfig> rawPage = llmProviderConfigMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()),
                wrapper
        );
        List<LlmProviderVO> records = rawPage.getRecords().stream()
                .map(this::toProviderVO)
                .toList();

        return new LlmProviderPageVO(
                rawPage.getCurrent(),
                Math.toIntExact(rawPage.getSize()),
                rawPage.getTotal(),
                rawPage.getPages(),
                records
        );
    }

    @Override
    public LlmProviderVO getProvider(String providerId) {
        // 纯 CRUD：只查询详情响应需要的字段，逻辑删除条件由 MyBatis-Plus 自动追加。
        String normalizedProviderId = verifyProviderId(providerId);
        LlmProviderConfig provider = llmProviderConfigMapper.selectOne(
                new LambdaQueryWrapper<LlmProviderConfig>()
                        .select(
                                LlmProviderConfig::getId,
                                LlmProviderConfig::getBaseUrl,
                                LlmProviderConfig::getModel,
                                LlmProviderConfig::getModelType,
                                LlmProviderConfig::getEnabled,
                                LlmProviderConfig::getVersion,
                                LlmProviderConfig::getCreatedAt,
                                LlmProviderConfig::getUpdatedAt
                        )
                        .eq(LlmProviderConfig::getId, normalizedProviderId)
        );
        if (provider == null) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_NOT_FOUND);
        }

        // 返回固定密钥掩码，避免密文进入接口响应。
        return toProviderVO(provider);
    }

    @Override
    @Transactional
    public LlmProviderVO updateProvider(String providerId, LlmProviderUpdateReq req) {
        // 校验至少提交一个更新字段，并规范化每个已提交的值。
        String normalizedProviderId = verifyProviderId(providerId);
        if (req.getBaseUrl() == null && req.getApiKey() == null && req.getModel() == null) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "至少提交 baseUrl、apiKey 或 model 之一");
        }
        String baseUrl = req.getBaseUrl() == null ? null : verifyBaseUrl(req.getBaseUrl());
        String model = req.getModel() == null ? null : verifyModel(req.getModel());
        String encryptedApiKey = req.getApiKey() == null
                ? null
                : secretCipher.encrypt(verifyApiKey(req.getApiKey()));

        // 先区分记录不存在和客户端版本过期，再由 UPDATE 的版本条件兜住查询后的并发修改。
        LlmProviderConfig current = requireActiveProvider(normalizedProviderId);
        if (!Objects.equals(current.getVersion(), req.getExpectedVersion())) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_VERSION_CONFLICT);
        }

        // 半量更新使用实体承载字段，XML 显式执行 version+1 并写入审计信息。
        LlmProviderConfig update = auditedProvider(normalizedProviderId);
        update.setBaseUrl(baseUrl);
        update.setApiKeyCiphertext(encryptedApiKey);
        update.setModel(model);
        int affected = llmProviderConfigMapper.updateProviderByVersion(update, req.getExpectedVersion());
        if (affected != 1) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_VERSION_CONFLICT);
        }

        return toProviderVO(requireActiveProvider(normalizedProviderId));
    }

    @Override
    @Transactional
    public LlmProviderStatusVO updateProviderStatus(String providerId, LlmProviderStatusReq req) {
        String normalizedProviderId = verifyProviderId(providerId);

        // 停用前按全局路由、启用场景、Provider 的顺序加锁并检查引用。
        List<AiGlobalRoute> routes = Boolean.FALSE.equals(req.getEnabled())
                ? aiGlobalRouteMapper.lockAll()
                : List.of();
        List<LlmSceneConfig> scenes = Boolean.FALSE.equals(req.getEnabled())
                ? llmSceneConfigMapper.lockEnabledScenesByProviderId(normalizedProviderId)
                : List.of();
        LlmProviderConfig provider = requireLockedActiveProvider(normalizedProviderId);
        if (!Objects.equals(provider.getVersion(), req.getExpectedVersion())) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_VERSION_CONFLICT);
        }

        // 默认路由仍引用 Provider 时禁止停用，避免提交后出现无可用默认模型。
        if (Boolean.FALSE.equals(req.getEnabled())
                && isReferencedByRoute(routes, scenes, normalizedProviderId)) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_IN_USE);
        }

        // 单字段状态更新显式执行 CAS、版本递增和审计字段维护。
        LlmProviderConfig update = auditedProvider(normalizedProviderId);
        update.setEnabled(req.getEnabled());
        int affected = llmProviderConfigMapper.updateProviderStatusByVersion(update, req.getExpectedVersion());
        if (affected != 1) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_VERSION_CONFLICT);
        }

        LlmProviderConfig latest = requireActiveProvider(normalizedProviderId);
        // TODO 路由缓存完成后，在事务提交后发布 Provider 配置变更事件。
        return new LlmProviderStatusVO(
                latest.getId(),
                latest.getEnabled(),
                latest.getVersion(),
                latest.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public LlmProviderDeleteVO deleteProvider(String providerId, Integer expectedVersion) {
        String normalizedProviderId = verifyProviderId(providerId);

        // 删除遵守全局路由、启用场景、Provider 的固定加锁顺序。
        List<AiGlobalRoute> routes = aiGlobalRouteMapper.lockAll();
        List<LlmSceneConfig> scenes =
                llmSceneConfigMapper.lockEnabledScenesByProviderId(normalizedProviderId);
        LlmProviderConfig provider = requireLockedActiveProvider(normalizedProviderId);
        if (!Objects.equals(provider.getVersion(), expectedVersion)) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_VERSION_CONFLICT);
        }
        if (isReferencedByRoute(routes, scenes, normalizedProviderId)) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_IN_USE);
        }

        // 逻辑删除同时关闭路由开关，并通过版本条件防止覆盖并发修改。
        LlmProviderConfig update = auditedProvider(normalizedProviderId);
        int affected = llmProviderConfigMapper.deleteProviderByVersion(update, expectedVersion);
        if (affected != 1) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_VERSION_CONFLICT);
        }

        // TODO 路由缓存完成后，在事务提交后发布 Provider 删除事件。
        return new LlmProviderDeleteVO(normalizedProviderId);
    }

    @Override
    public LlmProviderTestConnectionVO testConnection(String providerId) {
        String normalizedProviderId = verifyProviderId(providerId);

        // 允许测试未启用的配置，但逻辑删除或不存在的配置不可测试。
        LlmProviderConfig provider = requireActiveProvider(normalizedProviderId);
        if (provider.getModelType() != AiModelType.CHAT) {
            // ponytail: 当前仅有 OpenAI 兼容 Chat 测试适配器；对应类型适配器落地后再开放其他连接测试。
            throw new BusinessException(ErrorCode.AI_PROVIDER_TEST_UNSUPPORTED);
        }
        String plainApiKey;
        try {
            plainApiKey = secretCipher.decrypt(provider.getApiKeyCiphertext());
        } catch (SecretCipherException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.AI_API_KEY_INVALID);
        }
        if (StrUtil.isBlank(plainApiKey)) {
            throw new BusinessException(ErrorCode.AI_API_KEY_INVALID);
        }

        // 使用独立短超时和无重试客户端，避免连接测试污染运行时路由客户端或放大外部调用。
        long startedAt = System.nanoTime();
        try {
            OpenAiChatModel chatModel = createTestChatModel(provider, plainApiKey);
            ChatResponse response = chatModel.call(new Prompt("Reply only with OK."));
            if (response.getResult() == null) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_CONNECTION_FAILED);
            }

            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            return new LlmProviderTestConnectionVO(
                    provider.getId(),
                    provider.getModel(),
                    true,
                    latencyMs,
                    OffsetDateTime.now()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw mapConnectionException(exception);
        } finally {
            plainApiKey = null;
        }
    }

    /**
     * 按受支持的字段应用稳定排序。
     */
    private void applySorting(LambdaQueryWrapper<LlmProviderConfig> wrapper,
                              String requestedSort,
                              String requestedOrder) {
        String sort = StrUtil.isBlank(requestedSort) ? "createdAt" : requestedSort;
        boolean ascending = "asc".equalsIgnoreCase(requestedOrder);

        switch (sort) {
            case "providerId" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getId);
            case "baseUrl" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getBaseUrl);
            case "model" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getModel);
            case "modelType" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getModelType);
            case "enabled" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getEnabled);
            case "updatedAt" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getUpdatedAt);
            default -> wrapper.orderBy(true, ascending, LlmProviderConfig::getCreatedAt);
        }
        if (!"providerId".equals(sort)) {
            wrapper.orderByAsc(LlmProviderConfig::getId);
        }
    }

    /**
     * 将 Provider 实体转换为不暴露密文的响应。
     */
    private LlmProviderVO toProviderVO(LlmProviderConfig provider) {
        return new LlmProviderVO(
                provider.getId(),
                provider.getBaseUrl(),
                MASKED_API_KEY,
                provider.getModel(),
                provider.getModelType(),
                provider.getEnabled(),
                provider.getVersion(),
                provider.getCreatedAt(),
                provider.getUpdatedAt()
        );
    }

    /**
     * 新增一条尚未逻辑删除的 Provider。
     */
    private void insertProvider(String providerId,
                                String baseUrl,
                                String model,
                                AiModelType modelType,
                                String encryptedApiKey,
                                Boolean enabled) {
        LlmProviderConfig provider = LlmProviderConfig.builder()
                .id(providerId)
                .baseUrl(baseUrl)
                .model(model)
                .modelType(modelType)
                .apiKeyCiphertext(encryptedApiKey)
                .enabled(Boolean.TRUE.equals(enabled))
                .build();
        try {
            if (llmProviderConfigMapper.insert(provider) != 1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ALREADY_EXISTS);
        }
    }

    /**
     * 使用本次提交的完整配置恢复逻辑删除记录。
     */
    private void restoreProvider(LlmProviderConfig existing,
                                 String baseUrl,
                                 String model,
                                 AiModelType modelType,
                                 String encryptedApiKey,
                                 Boolean enabled) {
        LlmProviderConfig provider = auditedProvider(existing.getId());
        provider.setBaseUrl(baseUrl);
        provider.setModel(model);
        provider.setModelType(modelType);
        provider.setApiKeyCiphertext(encryptedApiKey);
        provider.setEnabled(Boolean.TRUE.equals(enabled));

        int affected = llmProviderConfigMapper.restoreDeletedProvider(provider, existing.getVersion());
        if (affected != 1) {
            LlmProviderConfig latest = llmProviderConfigMapper.selectByIdIncludingDeleted(existing.getId());
            if (latest != null && !Boolean.TRUE.equals(latest.getIsDeleted())) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_ALREADY_EXISTS);
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 查询有效 Provider 并统一处理不存在和逻辑删除。
     */
    private LlmProviderConfig requireActiveProvider(String providerId) {
        LlmProviderConfig provider = llmProviderConfigMapper.selectByIdIncludingDeleted(providerId);
        if (provider == null || Boolean.TRUE.equals(provider.getIsDeleted())) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_NOT_FOUND);
        }
        return provider;
    }

    /**
     * 锁定有效 Provider 并统一处理不存在和逻辑删除。
     */
    private LlmProviderConfig requireLockedActiveProvider(String providerId) {
        LlmProviderConfig provider = llmProviderConfigMapper.lockByIdIncludingDeleted(providerId);
        if (provider == null || Boolean.TRUE.equals(provider.getIsDeleted())) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_NOT_FOUND);
        }
        return provider;
    }

    /**
     * 判断 Provider 是否仍被全局默认路由或启用场景直接引用。
     */
    private boolean isReferencedByRoute(List<AiGlobalRoute> routes,
                                        List<LlmSceneConfig> scenes,
                                        String providerId) {
        return routes.stream().anyMatch(route -> providerId.equals(route.getProviderId()))
                || !scenes.isEmpty();
    }

    /**
     * 创建只承载写入字段和审计信息的半量更新实体。
     */
    private LlmProviderConfig auditedProvider(String providerId) {
        return LlmProviderConfig.builder()
                .id(providerId)
                .updatedBy(AuthContext.getUserIdOrNull())
                .traceId(TraceUtil.getTraceId())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * 创建一次性、无重试的连接测试客户端。
     */
    private OpenAiChatModel createTestChatModel(LlmProviderConfig provider, String plainApiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECTION_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(plainApiKey)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(provider.getModel())
                .temperature(0D)
                .maxTokens(1)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .retryTemplate(new RetryTemplate(RetryPolicy.withMaxRetries(0)))
                .build();
    }

    /**
     * 将外部模型调用异常映射为稳定业务错误码。
     */
    private BusinessException mapConnectionException(Throwable exception) {
        Set<Throwable> visited = new HashSet<>();
        Throwable current = exception;
        while (current != null && visited.add(current)) {
            Integer statusCode = extractStatusCode(current);
            if (statusCode != null) {
                if (statusCode == 401 || statusCode == 403) {
                    return new BusinessException(ErrorCode.AI_API_KEY_INVALID);
                }
                if (statusCode == 404) {
                    return new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
                }
                if (statusCode == 408 || statusCode == 504) {
                    return new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT);
                }
            }
            if (current instanceof SocketTimeoutException
                    || current instanceof java.net.http.HttpTimeoutException
                    || current instanceof TimeoutException) {
                return new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT);
            }
            current = current.getCause();
        }
        return new BusinessException(ErrorCode.AI_PROVIDER_CONNECTION_FAILED);
    }

    /**
     * 从同步或响应式 HTTP 异常中提取状态码。
     */
    private Integer extractStatusCode(Throwable exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().value();
        }
        if (exception instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().value();
        }
        return null;
    }

    private String verifyModel(String model) {
        if (StrUtil.isBlank(model)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "model为空或格式错误");
        }
        String normalizedModel = model.trim();
        if (normalizedModel.length() > 128) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "model过长");
        }
        return normalizedModel;
    }

    private String verifyBaseUrl(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "baseUrl为空或格式错误");
        }
        String normalizedBaseUrl = baseUrl.trim().replaceFirst("/+$", "");
        if (normalizedBaseUrl.length() > 512) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "baseUrl过长");
        }

        try {
            URI uri = new URI(normalizedBaseUrl);
            boolean validProtocol = "http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme());
            if (!validProtocol
                    || StrUtil.isBlank(uri.getHost())
                    || uri.getPort() > 65535
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "baseUrl格式不合法");
            }
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "baseUrl格式不合法");
        }
        return normalizedBaseUrl;
    }

    private String verifyProviderId(String providerId) {
        if (StrUtil.isBlank(providerId)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "providerId为空或格式错误");
        }
        String normalizedProviderId = providerId.trim().toLowerCase(Locale.ROOT);
        if (!PROVIDER_ID_PATTERN.matcher(normalizedProviderId).matches()) {
            throw new BusinessException(
                    ErrorCode.PARAM_VALID_ERROR,
                    "providerId仅支持小写字母、数字、点、下划线和连字符，且最长64位"
            );
        }
        return normalizedProviderId;
    }

    private String verifyApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "API Key不能为空");
        }
        return apiKey.trim();
    }
}
