package interview.data.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * API 访问日志展示项（热表与归档冷表共用）。
 */
@Schema(description = "API 访问日志展示项")
public record ApiLogListItemVO(
        @Schema(description = "日志 ID")
        Long id,
        @Schema(description = "分布式链路追踪 ID")
        String traceId,
        @Schema(description = "触发请求的用户 ID")
        Long userId,
        @Schema(description = "操作人用户名（批量补齐）")
        String username,
        @Schema(description = "请求的接口路径")
        String apiPath,
        @Schema(description = "HTTP 动词", example = "GET")
        String requestMethod,
        @Schema(description = "客户端真实 IP")
        String clientIp,
        @Schema(description = "接口执行耗时（毫秒）")
        Long executionTimeMs,
        @Schema(description = "HTTP 响应状态码", example = "200")
        Integer responseStatus,
        @Schema(description = "简要异常信息（已脱敏）")
        String errorMessage,
        @Schema(description = "LLM 输入 Token 数")
        Integer llmInputTokens,
        @Schema(description = "LLM 输出 Token 数")
        Integer llmOutputTokens,
        @Schema(description = "使用的模型名称")
        String llmModel,
        @Schema(description = "日志生成时间")
        OffsetDateTime createdAt
) {
}
