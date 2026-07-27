package interview.ai.providerconfig.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.ai.providerconfig.mapper.LlmGlobalSettingMapper;
import interview.ai.providerconfig.model.entity.LlmGlobalSetting;
import interview.ai.providerconfig.model.vo.LlmSettingVO;
import interview.common.exception.BusinessException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmGlobalSettingServiceImplTest {

    @Mock
    private LlmGlobalSettingMapper settingMapper;

    private LlmGlobalSettingServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        // 单元测试环境手动初始化 LambdaWrapper 所需的实体元数据。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                LlmGlobalSetting.class
        );
    }

    @BeforeEach
    void setUp() {
        service = new LlmGlobalSettingServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", settingMapper);
    }

    @Test
    void getSetting_shouldReturnSingletonSetting() {
        // 构造 id=1 的全局路由单例。
        LlmGlobalSetting setting = LlmGlobalSetting.builder()
                .id(1L)
                .defaultChatProviderId("dashscope-chat")
                .defaultEmbeddingProviderId("openai-embedding")
                .version(3)
                .updatedAt(OffsetDateTime.now())
                .build();
        when(settingMapper.selectOne(any())).thenReturn(setting);

        LlmSettingVO result = service.getSetting();

        assertEquals("1", result.id());
        assertEquals("dashscope-chat", result.defaultChatProviderId());
        assertEquals("openai-embedding", result.defaultEmbeddingProviderId());
        assertEquals(3, result.version());
    }

    @Test
    void getSetting_shouldRejectMissingSingleton() {
        // 单例尚未初始化时返回统一业务异常。
        when(settingMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                service::getSetting
        );

        assertEquals(120009, exception.getCode());
    }
}
