package interview.rag.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * RAG 消息游标分页查询参数。
 */
@Data
@Schema(description = "RAG 消息游标分页查询参数")
public class RagMessageCursorReq {

    @Min(value = 0, message = "游标不能小于 0")
    @Schema(description = "游标（上一页最后一条消息 ID）；为空时从最早消息开始", example = "1934567890123456")
    private Long cursor;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "50")
    private Integer size = 50;
}
