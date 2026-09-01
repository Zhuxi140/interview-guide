package interview.ai.config.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.ai.config.Listener.LLMGlobalRouteChangeListener;
import interview.ai.config.mapper.AiGlobalRouteMapper;
import interview.ai.config.mapper.LlmProviderConfigMapper;
import interview.ai.config.model.entity.AiGlobalRoute;
import interview.ai.config.model.entity.LlmProviderConfig;
import interview.ai.config.model.req.AiGlobalRouteUpdateReq;
import interview.ai.config.model.vo.AiGlobalRouteVO;
import interview.common.enums.AiModelType;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGlobalRouteServiceImplTest {

    @Mock
    private AiGlobalRouteMapper routeMapper;

    @Mock
    private LlmProviderConfigMapper providerMapper;

    @Mock
    private ApplicationEventPublisher listener;

    private AiGlobalRouteServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                AiGlobalRoute.class
        );
    }

    @BeforeEach
    void setUp() {
        service = new AiGlobalRouteServiceImpl(providerMapper, listener);
        ReflectionTestUtils.setField(service, "baseMapper", routeMapper);
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(1L)
                .userType(UserType.PLATFORM_ADMIN)
                .build());
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    @Test
    void listRoutes_shouldReturnAllRouteSlots() {
        when(routeMapper.selectList(any())).thenReturn(List.of(
                route(AiModelType.CHAT, "dashscope-chat", 2),
                route(AiModelType.TTS, null, 0)
        ));

        var result = service.listRoutes();

        assertEquals(2, result.routes().size());
        assertEquals(AiModelType.CHAT, result.routes().getFirst().modelType());
        assertEquals(null, result.routes().get(1).providerId());
    }

    @Test
    void updateRoute_shouldValidateProviderTypeAndUseCas() {
        AiGlobalRoute current = route(AiModelType.CHAT, null, 2);
        AiGlobalRoute latest = route(AiModelType.CHAT, "dashscope-chat", 3);
        when(routeMapper.lockByModelType(AiModelType.CHAT)).thenReturn(current);
        when(providerMapper.lockByIdIncludingDeleted("dashscope-chat"))
                .thenReturn(provider(AiModelType.CHAT));
        when(routeMapper.updateRouteByVersion(any(), eq(2))).thenReturn(1);
        when(routeMapper.selectById(AiModelType.CHAT)).thenReturn(latest);

        AiGlobalRouteUpdateReq req = new AiGlobalRouteUpdateReq();
        req.setProviderId(" DashScope-Chat ");
        req.setExpectedVersion(2);

        AiGlobalRouteVO result = service.updateRoute(AiModelType.CHAT, req);

        assertEquals("dashscope-chat", result.providerId());
        assertEquals(3, result.version());
    }

    @Test
    void updateRoute_shouldRejectProviderTypeMismatch() {
        when(routeMapper.lockByModelType(AiModelType.CHAT))
                .thenReturn(route(AiModelType.CHAT, null, 0));
        when(providerMapper.lockByIdIncludingDeleted("embedding"))
                .thenReturn(provider(AiModelType.EMBEDDING));

        AiGlobalRouteUpdateReq req = new AiGlobalRouteUpdateReq();
        req.setProviderId("embedding");
        req.setExpectedVersion(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateRoute(AiModelType.CHAT, req)
        );

        assertEquals(120021, exception.getCode());
        verify(routeMapper, never()).updateRouteByVersion(any(), any());
    }

    private AiGlobalRoute route(AiModelType modelType, String providerId, int version) {
        return AiGlobalRoute.builder()
                .modelType(modelType)
                .providerId(providerId)
                .version(version)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private LlmProviderConfig provider(AiModelType modelType) {
        return LlmProviderConfig.builder()
                .id("provider")
                .modelType(modelType)
                .enabled(true)
                .isDeleted(false)
                .build();
    }
}
