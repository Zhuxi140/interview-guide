package interview.tutor.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 答疑消息游标分页结果。
 */
@Schema(description = "答疑消息游标分页结果")
public record TutorMessagePageVO(
        @Schema(description = "下页游标（最后一条消息 ID），无更多数据时为 null", example = "193882716")
        Long nextCursor,
        @Schema(description = "是否还有更多消息")
        boolean hasMore,
        @Schema(description = "本页消息列表")
        List<TutorMessageListItemVO> records
) {
}
