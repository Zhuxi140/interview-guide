package interview.rag.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * RAG 会话绑定知识库响应（完整替换后的生效集合）。
 */
@Schema(description = "RAG 会话绑定知识库响应")
public record RagSessionKnowledgeBindVO(
        @Schema(description = "会话 ID")
        Long sessionId,
        @Schema(description = "绑定生效的知识库 ID 集合（已去重）", example = "[1001, 1002]")
        List<Long> knowledgeBaseIds
) {
}
