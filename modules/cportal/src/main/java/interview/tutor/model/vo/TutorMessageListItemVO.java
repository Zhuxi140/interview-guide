package interview.tutor.model.vo;

import interview.tutor.model.enums.TutorMessageRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 答疑消息列表项。
 */
@Schema(description = "答疑消息列表项")
public record TutorMessageListItemVO(
        @Schema(description = "消息 ID")
        Long id,
        @Schema(description = "消息角色", example = "USER")
        TutorMessageRole role,
        @Schema(description = "消息正文")
        String content,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
