package interview.ai.config.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.ai.config.mapper.AiGlobalRouteMapper;
import interview.ai.config.mapper.LlmProviderConfigMapper;
import interview.ai.config.mapper.LlmSceneConfigMapper;
import interview.ai.config.model.entity.AiGlobalRoute;
import interview.ai.config.model.entity.LlmProviderConfig;
import interview.ai.config.model.req.LlmProviderCreateReq;
import interview.ai.config.model.req.LlmProviderQueryReq;
import interview.ai.config.model.req.LlmProviderStatusReq;
import interview.ai.config.model.req.LlmProviderUpdateReq;
import interview.ai.config.model.vo.LlmProviderPageVO;
import interview.ai.config.model.vo.LlmProviderVO;
import interview.common.enums.AiModelType;
import interview.common.exception.BusinessException;
import interview.common.security.SecretCipher;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmProviderConfigServiceImplTest {

    @Mock
    private LlmProviderConfigMapper providerMapper;

    @Mock
    private AiGlobalRouteMapper routeMapper;

    @Mock
    private LlmSceneConfigMapper sceneMapper;

    @Mock
    private SecretCipher secretCipher;

    private LlmProviderConfigServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        // 单元测试环境手动初始化 LambdaWrapper 所需的实体元数据。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                LlmProviderConfig.class
        );
    }

    @BeforeEach
    void setUp() {
        service = new LlmProviderConfigServiceImpl(
                providerMapper,
                routeMapper,
                sceneMapper,
                secretCipher
        );
        ReflectionTestUtils.setField(service, "baseMapper", providerMapper);
    }

    @Test
    void pageProviders_shouldReturnPageAndHideCiphertext() {
        // 构造包含密文的数据库分页结果。
        LlmProviderConfig provider = provider();
        Page<LlmProviderConfig> rawPage = new Page<>(1, 20, 1);
        rawPage.setRecords(List.of(provider));
        when(providerMapper.selectPage(any(), any())).thenReturn(rawPage);

        LlmProviderPageVO result = service.pageProviders(new LlmProviderQueryReq());

        // 响应保留分页信息，但不能包含数据库密文。
        LlmProviderVO item = result.records().getFirst();
        assertEquals(1L, result.total());
        assertEquals("dashscope-chat", item.providerId());
        assertEquals("******", item.apiKeyMasked());
        assertFalse(item.apiKeyMasked().contains(provider.getApiKeyCiphertext()));
    }

    @Test
    void getProvider_shouldReturnMaskedDetail() {
        // 查询到 Provider 时转换为脱敏详情。
        when(providerMapper.selectOne(any())).thenReturn(provider());

        LlmProviderVO result = service.getProvider("dashscope-chat");

        assertEquals("dashscope-chat", result.providerId());
        assertEquals("qwen-max", result.model());
        assertEquals("******", result.apiKeyMasked());
    }

    @Test
    void getProvider_shouldRejectMissingProvider() {
        // Provider 不存在时返回统一业务异常。
        when(providerMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getProvider("missing-provider")
        );

        assertEquals(120001, exception.getCode());
    }

    @Test
    void createProvider_shouldRestoreDeletedProviderWithNewSecret() {
        // 同名逻辑删除记录使用当前配置恢复，不复用旧密钥。
        LlmProviderConfig deleted = provider();
        deleted.setIsDeleted(true);
        deleted.setVersion(4);
        LlmProviderConfig restored = provider();
        restored.setVersion(5);
        when(providerMapper.selectByIdIncludingDeleted("dashscope-chat"))
                .thenReturn(deleted, restored);
        when(secretCipher.encrypt("new-secret")).thenReturn("new-ciphertext");
        when(providerMapper.restoreDeletedProvider(any(), eq(4))).thenReturn(1);

        LlmProviderCreateReq req = new LlmProviderCreateReq();
        req.setProviderId("DashScope-Chat");
        req.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/");
        req.setApiKey("new-secret");
        req.setModel("qwen-max");
        req.setModelType(AiModelType.CHAT);
        req.setEnabled(true);

        LlmProviderVO result = service.createProvider(req);

        ArgumentCaptor<LlmProviderConfig> updateCaptor = ArgumentCaptor.forClass(LlmProviderConfig.class);
        verify(providerMapper).restoreDeletedProvider(updateCaptor.capture(), eq(4));
        assertEquals("new-ciphertext", updateCaptor.getValue().getApiKeyCiphertext());
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode", updateCaptor.getValue().getBaseUrl());
        assertEquals(5, result.version());
    }

    @Test
    void updateProvider_shouldUseVersionCasAndReturnLatestRecord() {
        // 查询时版本正确，更新 SQL 仍携带 expectedVersion 防止查询后的并发覆盖。
        LlmProviderConfig current = provider();
        LlmProviderConfig latest = provider();
        latest.setModel("qwen-plus");
        latest.setVersion(3);
        when(providerMapper.selectByIdIncludingDeleted("dashscope-chat"))
                .thenReturn(current, latest);
        when(providerMapper.updateProviderByVersion(any(), eq(2))).thenReturn(1);

        LlmProviderUpdateReq req = new LlmProviderUpdateReq();
        req.setExpectedVersion(2);
        req.setModel(" qwen-plus ");

        LlmProviderVO result = service.updateProvider("dashscope-chat", req);

        ArgumentCaptor<LlmProviderConfig> updateCaptor = ArgumentCaptor.forClass(LlmProviderConfig.class);
        verify(providerMapper).updateProviderByVersion(updateCaptor.capture(), eq(2));
        assertEquals("qwen-plus", updateCaptor.getValue().getModel());
        assertEquals(3, result.version());
    }

    @Test
    void updateProviderStatus_shouldRejectDisablingReferencedProvider() {
        // 全局默认路由仍引用 Provider 时，停用请求必须被拒绝。
        when(routeMapper.lockAll()).thenReturn(List.of(
                AiGlobalRoute.builder()
                        .modelType(AiModelType.CHAT)
                        .providerId("dashscope-chat")
                        .version(1)
                        .build()
        ));
        when(sceneMapper.lockEnabledScenesByProviderId("dashscope-chat")).thenReturn(List.of());
        when(providerMapper.lockByIdIncludingDeleted("dashscope-chat")).thenReturn(provider());

        LlmProviderStatusReq req = new LlmProviderStatusReq();
        req.setExpectedVersion(2);
        req.setEnabled(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateProviderStatus("dashscope-chat", req)
        );

        assertEquals(120015, exception.getCode());
        verify(providerMapper, never()).updateProviderStatusByVersion(any(), any());
    }

    @Test
    void deleteProvider_shouldRejectStaleVersion() {
        // 删除前先完成固定顺序加锁，过期版本不能覆盖最新配置。
        when(routeMapper.lockAll()).thenReturn(List.of());
        when(sceneMapper.lockEnabledScenesByProviderId("dashscope-chat")).thenReturn(List.of());
        when(providerMapper.lockByIdIncludingDeleted("dashscope-chat")).thenReturn(provider());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.deleteProvider("dashscope-chat", 1)
        );

        assertEquals(120014, exception.getCode());
        verify(providerMapper, never()).deleteProviderByVersion(any(), any());
    }

    private LlmProviderConfig provider() {
        OffsetDateTime now = OffsetDateTime.now();
        return LlmProviderConfig.builder()
                .id("dashscope-chat")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .apiKeyCiphertext("ciphertext-must-not-leak")
                .model("qwen-max")
                .modelType(AiModelType.CHAT)
                .enabled(true)
                .version(2)
                .isDeleted(false)
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build();
    }
}
