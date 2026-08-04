package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "面试时间线分页响应（游标式）")
public record InterviewTimelinePageVO(
        @Schema(description = "最后一条事件的序号", example = "15")
        Long lastSequence,

        @Schema(description = "是否还有更多事件")
        Boolean hasMore,

        @Schema(description = "事件列表")
        List<InterviewTimelineEventVO> events
) {
}