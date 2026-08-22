package interview.rag.model.vo;

import interview.rag.model.enums.RagMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * RAG 对话消息项响应。
 */
@Schema(description = "RAG 对话消息项响应")
public record RagMessageVO(
        @Schema(description = "消息 ID（同时作为下一页游标）")
        Long id,
        @Schema(description = "消息类型：USER 用户消息 / AI 回答")
        RagMessageType type,
        @Schema(description = "消息原文")
        String content,
        @Schema(description = "发送时间")
        OffsetDateTime createdAt
) {
}
