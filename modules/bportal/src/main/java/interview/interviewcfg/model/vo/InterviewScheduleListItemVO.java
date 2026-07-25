package interview.interviewcfg.model.vo;

import interview.common.enums.InterviewScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "面试排期列表项")
public record InterviewScheduleListItemVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "投递记录ID")
        Long applicationId,

        @Schema(description = "候选人姓名")
        String candidateName,

        @Schema(description = "岗位名称")
        String jobTitle,

        @Schema(description = "面试时间")
        OffsetDateTime interviewTime,

        @Schema(description = "面试类型 TEXT / VOICE / CODE")
        String interviewType,

        @Schema(description = "排期状态")
        InterviewScheduleStatus status,

        @Schema(description = "乐观锁版本号")
        Integer version
) {
}
