package interview.api.aicore.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * AI 面试编排草案结果。
 *
 * @param stages 阶段计划，与输入快照阶段集合及顺序一致
 * @param scheduleSuggestions 排期建议，可为空
 * @param llmConfigSnapshot 实际使用的 LLM 配置快照，不含密钥
 */
public record AiInterviewPlanResult(
        List<Stage> stages,
        List<ScheduleSuggestion> scheduleSuggestions,
        LlmConfigSnapshotDTO llmConfigSnapshot
) {

    /**
     * 单阶段计划。
     */
    public record Stage(
            String phaseCode,
            String objectives,
            String questionOutline,
            Integer durationMinutes
    ) {
    }

    /**
     * 单条排期建议。
     */
    public record ScheduleSuggestion(
            Long suggestionId,
            Long interviewerUserId,
            OffsetDateTime interviewTime,
            String reason
    ) {
    }
}