package interview.rag.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * RAG 会话列表项响应。
 */
@Builder
@Schema(description = "RAG 会话列表项响应")
public record RagSessionListItemVO(
        @Schema(description = "会话 ID")
        Long id,
        @Schema(description = "会话标题")
        String title,
        @Schema(description = "会话消息数量", example = "12")
        Long messageCount,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
