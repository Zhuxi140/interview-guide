package interview.api.aicore.dto;

import lombok.Builder;

import java.util.List;

/**
 * AI 简历分析结果
 */
@Builder
public record AiResumeAnalysisResult(
    /**
     * 综合评分（0-100）
     */
    Integer overallScore,

    /**
     * 优势项 JSON 数组字符串
     */
    List<String> strengthsJson,

    /**
     * 改进建议 JSON 数组字符串
     */
    List<String> suggestionsJson,

    /**
     *  非敏感配置快照
     */
    LlmConfigSnapshotDTO llmConfigSnapshot

) {

}
