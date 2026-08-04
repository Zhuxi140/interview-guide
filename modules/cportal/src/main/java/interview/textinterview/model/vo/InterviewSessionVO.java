package interview.textinterview.model.vo;

import interview.common.enums.InterviewSessionStatus;
import interview.common.enums.InterviewType;
import interview.voiceinterview.model.vo.VoiceDetailsVO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "面试会话状态")
public record InterviewSessionVO(
        @Schema(description = "会话ID")
        Long id,

        @Schema(description = "排期ID")
        Long scheduleId,

        @Schema(description = "会话类型", example = "TEXT")
        InterviewType sessionType,

        @Schema(description = "当前尝试次数", example = "1")
        Short attemptNo,

        @Schema(description = "会话状态 CREATED / IN_PROGRESS / COMPLETED")
        InterviewSessionStatus status,

        @Schema(description = "会话最后事件序号")
        Long lastEventSequence,

        @Schema(description = "语音扩展信息，文本会话为空", nullable = true)
        VoiceDetailsVO voiceDetails,

        @Schema(description = "开始时间", nullable = true)
        OffsetDateTime startedAt,

        @Schema(description = "结束时间", nullable = true)
        OffsetDateTime endedAt
) {
}
