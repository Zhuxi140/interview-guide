package interview.ai.config.service.impl;

import interview.ai.config.mapper.AiGlobalRouteMapper;
import interview.ai.config.mapper.LlmProviderConfigMapper;
import interview.ai.config.mapper.LlmSceneConfigMapper;
import interview.ai.config.model.entity.AiGlobalRoute;
import interview.ai.config.model.entity.LlmProviderConfig;
import interview.ai.config.model.entity.LlmSceneConfig;
import interview.ai.config.model.req.LlmSceneStatusReq;
import interview.ai.config.model.req.LlmSceneUpdateReq;
import interview.ai.config.model.vo.LlmSceneStatusVO;
import interview.ai.config.model.vo.LlmSceneVO;
import interview.common.enums.AiModelType;
import interview.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmSceneConfigServiceImplTest {

    @Mock
    private LlmSceneConfigMapper sceneMapper;

    @Mock
    private AiGlobalRouteMapper routeMapper;

    @Mock
    private LlmProviderConfigMapper providerMapper;

    private LlmSceneConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LlmSceneConfigServiceImpl(sceneMapper, routeMapper, providerMapper);
    }

    @Test
    void updateScene_shouldUseExplicitProviderAndCas() {
        LlmSceneConfig current = scene(false, 2, "old-provider");
        LlmSceneConfig latest = scene(false, 3, "dashscope-chat");
        when(sceneMapper.lockBySceneCode("RESUME_ANALYSIS")).thenReturn(current);
        when(providerMapper.lockByIdIncludingDeleted("dashscope-chat"))
                .thenReturn(provider("dashscope-chat"));
        when(sceneMapper.updateSceneByVersion(any(), eq(2))).thenReturn(1);
        when(sceneMapper.selectById("RESUME_ANALYSIS")).thenReturn(latest);

        LlmSceneUpdateReq req = validUpdateReq();
        req.setProviderId(" DashScope-Chat ");

        LlmSceneVO result = service.updateScene("resume_analysis", req);

        ArgumentCaptor<LlmSceneConfig> updateCaptor = ArgumentCaptor.forClass(LlmSceneConfig.class);
        verify(sceneMapper).updateSceneByVersion(updateCaptor.capture(), eq(2));
        verify(routeMapper, never()).lockByModelType(any());
        assertEquals("dashscope-chat", updateCaptor.getValue().getProviderId());
        assertEquals(3, result.version());
    }

    @Test
    void updateSceneStatus_shouldLockGlobalSceneAndProviderWhenEnablingDefaultRoute() {
        LlmSceneConfig current = scene(false, 3, null);
        LlmSceneConfig latest = scene(true, 4, null);
        AiGlobalRoute route = AiGlobalRoute.builder()
                .modelType(AiModelType.CHAT)
                .providerId("dashscope-chat")
                .version(1)
                .build();
        when(sceneMapper.selectById("RESUME_ANALYSIS")).thenReturn(current, latest);
        when(routeMapper.lockByModelType(AiModelType.CHAT)).thenReturn(route);
        when(sceneMapper.lockBySceneCode("RESUME_ANALYSIS")).thenReturn(current);
        when(providerMapper.lockByIdIncludingDeleted("dashscope-chat"))
                .thenReturn(provider("dashscope-chat"));
        when(sceneMapper.updateSceneStatusByVersion(any(), eq(3))).thenReturn(1);

        LlmSceneStatusReq req = new LlmSceneStatusReq();
        req.setExpectedVersion(3);
        req.setEnabled(true);

        LlmSceneStatusVO result = service.updateSceneStatus("RESUME_ANALYSIS", req);

        InOrder locks = inOrder(routeMapper, sceneMapper, providerMapper);
        locks.verify(routeMapper).lockByModelType(AiModelType.CHAT);
        locks.verify(sceneMapper).lockBySceneCode("RESUME_ANALYSIS");
        locks.verify(providerMapper).lockByIdIncludingDeleted("dashscope-chat");
        assertEquals(4, result.version());
        assertEquals(true, result.enabled());
    }

    @Test
    void updateScene_shouldRejectUnwhitelistedExtraOptions() {
        LlmSceneUpdateReq req = validUpdateReq();
        req.setExtraOptions("{\"seed\":1}");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateScene("RESUME_ANALYSIS", req)
        );

        assertEquals(120020, exception.getCode());
        verifyNoInteractions(sceneMapper, routeMapper, providerMapper);
    }

    private LlmSceneUpdateReq validUpdateReq() {
        LlmSceneUpdateReq req = new LlmSceneUpdateReq();
        req.setExpectedVersion(2);
        req.setTemperature(BigDecimal.valueOf(0.2));
        req.setTopP(BigDecimal.valueOf(0.9));
        req.setMaxInputTokens(16000);
        req.setMaxOutputTokens(2000);
        req.setTimeoutSeconds(60);
        req.setPromptVersion("resume-analysis-v1");
        req.setExtraOptions("{}");
        return req;
    }

    private LlmSceneConfig scene(boolean enabled, int version, String providerId) {
        return LlmSceneConfig.builder()
                .sceneCode("RESUME_ANALYSIS")
                .modelType(AiModelType.CHAT)
                .providerId(providerId)
                .temperature(BigDecimal.valueOf(0.2))
                .topP(BigDecimal.valueOf(0.9))
                .maxInputTokens(16000)
                .maxOutputTokens(2000)
                .timeoutSeconds(60)
                .promptVersion("resume-analysis-v1")
                .extraOptions("{}")
                .enabled(enabled)
                .version(version)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private LlmProviderConfig provider(String providerId) {
        return LlmProviderConfig.builder()
                .id(providerId)
                .modelType(AiModelType.CHAT)
                .enabled(true)
                .isDeleted(false)
                .version(1)
                .build();
    }
}
