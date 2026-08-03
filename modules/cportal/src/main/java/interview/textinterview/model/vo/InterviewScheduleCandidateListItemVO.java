package interview.textinterview.model.vo;

import interview.common.enums.InterviewScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "C端查看到的面试排期列表项")
public record InterviewScheduleCandidateListItemVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "企业名称")
        String enterpriseName,

        @Schema(description = "岗位名称")
        String jobTitle,

        @Schema(description = "当前投递下的面试轮次", example = "1")
        Short roundNo,

        @Schema(description = "由模板轮次派生的阶段编码", example = "TECHNICAL")
        String phaseCode,

        @Schema(description = "阶段名称", example = "技术面")
        String phaseName,

        @Schema(description = "面试时间")
        OffsetDateTime interviewTime,

        @Schema(description = "预计面试时长（分钟）", example = "60")
        Integer durationMinutes,

        @Schema(description = "面试类型 TEXT / VOICE / CODE")
        String interviewType,

        @Schema(description = "排期状态")
        InterviewScheduleStatus status,

        @Schema(description = "乐观锁版本号")
        Integer version
) {
}
