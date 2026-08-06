package interview.interviewcfg.model.vo;

import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "面试排期列表项")
public record InterviewScheduleListItemVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "投递记录ID")
        Long applicationId,

        @Schema(description = "当前投递下的面试轮次", example = "1")
        Short roundNo,

        @Schema(description = "由模板轮次派生的阶段编码", example = "TECHNICAL")
        String phaseCode,

        @Schema(description = "阶段名称", example = "技术面")
        String phaseName,

        @Schema(description = "候选人姓名")
        String candidateName,

        @Schema(description = "岗位名称")
        String jobTitle,

        @Schema(description = "面试时间")
        OffsetDateTime interviewTime,

        @Schema(description = "预计面试时长（分钟）", example = "60")
        Integer durationMinutes,

        @Schema(description = "面试类型 TEXT / VOICE")
        InterviewType interviewType,

        @Schema(description = "排期状态")
        InterviewScheduleStatus status,

        @Schema(description = "乐观锁版本号")
        Integer version
) {
}
