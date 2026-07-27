package interview.ai.providerconfig.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.ai.providerconfig.mapper.LlmProviderConfigMapper;
import interview.ai.providerconfig.model.entity.LlmProviderConfig;
import interview.ai.providerconfig.model.req.LlmProviderQueryReq;
import interview.ai.providerconfig.model.vo.LlmProviderPageVO;
import interview.ai.providerconfig.model.vo.LlmProviderVO;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmProviderConfigServiceImplTest {

    @Mock
    private LlmProviderConfigMapper providerMapper;

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
        service = new LlmProviderConfigServiceImpl();
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

    private LlmProviderConfig provider() {
        OffsetDateTime now = OffsetDateTime.now();
        return LlmProviderConfig.builder()
                .id("dashscope-chat")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .apiKeyCiphertext("ciphertext-must-not-leak")
                .model("qwen-max")
                .enabled(true)
                .version(2)
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build();
    }
}
