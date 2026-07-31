package interview.ai.llm.model;

import interview.api.aicore.dto.LlmConfigSnapshotDTO;

/**
 * 非敏感 LLM 配置快照转换。
 */
public final class LlmConfigSnapshotMapper {

    private LlmConfigSnapshotMapper() {
    }

    /**
     * 将运行时快照转换为跨模块 DTO。
     *
     * @param snapshot 运行时快照
     * @return 非敏感快照 DTO
     */
    public static LlmConfigSnapshotDTO toDto(LlmConfigSnapshot snapshot) {
        return LlmConfigSnapshotDTO.builder()
                .schemaVersion(snapshot.schemaVersion())
                .sceneCode(snapshot.sceneCode())
                .modelType(snapshot.modelType())
                .routeSource(snapshot.routeSource())
                .providerId(snapshot.providerId())
                .baseUrl(snapshot.baseUrl())
                .model(snapshot.model())
                .parameters(LlmConfigSnapshotDTO.LlmParametersSnapshot.builder()
                        .temperature(snapshot.parameters().temperature())
                        .topP(snapshot.parameters().topP())
                        .maxInputTokens(snapshot.parameters().maxInputTokens())
                        .maxOutputTokens(snapshot.parameters().maxOutputTokens())
                        .timeoutSeconds(snapshot.parameters().timeoutSeconds())
                        .extraOptions(snapshot.parameters().extraOptions())
                        .build())
                .prompt(new LlmConfigSnapshotDTO.PromptSnapshot(
                        snapshot.prompt().version(), snapshot.prompt().sha256()))
                .configVersions(new LlmConfigSnapshotDTO.ConfigVersions(
                        snapshot.configVersions().sceneConfigVersion(),
                        snapshot.configVersions().providerVersion(),
                        snapshot.configVersions().globalRouteVersion()))
                .resolvedAt(snapshot.resolvedAt())
                .build();
    }
}
