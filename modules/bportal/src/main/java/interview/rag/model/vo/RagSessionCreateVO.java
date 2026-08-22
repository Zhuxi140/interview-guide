package interview.rag.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * RAG 会话创建响应。
 */
@Builder
@Schema(description = "RAG 会话创建响应")
public record RagSessionCreateVO(
        @Schema(description = "会话 ID")
        Long id,
        @Schema(description = "归属企业 ID")
        Long enterpriseId,
        @Schema(description = "会话标题")
        String title,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
