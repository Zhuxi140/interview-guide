package interview.rag.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * RAG 消息游标分页响应。
 */
@Schema(description = "RAG 消息游标分页响应")
public record RagMessagePageVO(
        @Schema(description = "下一页游标（最后一条消息 ID）；hasMore 为 false 时为空", example = "1934567890123456")
        Long nextCursor,
        @Schema(description = "是否还有更多消息", example = "true")
        boolean hasMore,
        @Schema(description = "本页消息列表（按 id 升序）")
        List<RagMessageVO> records
) {
}
