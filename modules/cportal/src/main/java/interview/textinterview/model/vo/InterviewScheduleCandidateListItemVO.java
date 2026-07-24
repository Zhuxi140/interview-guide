package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "C端查看到的面试排期列表项")
public record InterviewScheduleCandidateListItemVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "企业名称")
        String enterpriseName,

        @Schema(description = "岗位名称")
        String jobTitle,

        @Schema(description = "面试时间")
        OffsetDateTime interviewTime,

        @Schema(description = "面试类型 TEXT / VOICE / CODE")
        String interviewType,

        @Schema(description = "排期状态")
        String status,

        @Schema(description = "乐观锁版本号")
        Integer version
) {
}
