package interview.ai.llm.model;

import interview.common.enums.AiModelType;
import interview.common.enums.AiSceneCode;
import interview.common.enums.RouteSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmConfigSnapshotDTOTest {

    @Test
    void shouldCreateSecretFreeImmutableSnapshot() {
        RouteResult route = RouteResult.builder()
                .sceneCode(AiSceneCode.RESUME_ANALYSIS)
                .modelType(AiModelType.CHAT)
                .providerId("dashscope-chat")
                .baseUrl("https://example.com/compatible-mode")
                .model("qwen-max")
                .apiKey("plain-secret")
                .temperature(new BigDecimal("0.3"))
                .topP(new BigDecimal("0.8"))
                .maxInputTokens(12_000)
                .maxOutputTokens(2_000)
                .timeoutSeconds(60)
                .promptVersion("v1")
                .sceneConfigVersion(3)
                .providerVersion(5)
                .globalRouteVersion(2)
                .extraOptions(Map.of("seed", 1))
                .build();

        // 快照应记录实际路由和提示词摘要，但结构上不存在任何 API Key 字段。
        LlmConfigSnapshot snapshot =
                LlmConfigSnapshot.from(route, "system prompt");
        LlmConfigSnapshot changedPrompt =
                LlmConfigSnapshot.from(route, "changed system prompt");

        assertEquals(RouteSource.GLOBAL, snapshot.routeSource());
        assertEquals("v1", snapshot.prompt().version());
        assertNotEquals(snapshot.prompt().sha256(), changedPrompt.prompt().sha256());
        assertFalse(Arrays.stream(LlmConfigSnapshot.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("apikey")));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.parameters().extraOptions().put("newKey", "newValue"));
    }
}
