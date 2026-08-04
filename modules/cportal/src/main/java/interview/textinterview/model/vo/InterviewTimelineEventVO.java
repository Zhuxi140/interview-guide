package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;

@Schema(description = "面试时间线事件")
public record InterviewTimelineEventVO(
        @Schema(description = "事件唯一ID", example = "evt_01abc123")
        String eventId,

        @Schema(description = "会话内严格递增序号", example = "1")
        Long sequence,

        @Schema(description = "事件类型", example = "QUESTION_GENERATED")
        String type,

        @Schema(description = "事件载荷")
        Map<String, Object> payload,

        @Schema(description = "服务端时间")
        OffsetDateTime serverTime
) {
}