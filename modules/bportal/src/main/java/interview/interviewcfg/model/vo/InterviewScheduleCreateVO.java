package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "创建面试排期响应")
public record InterviewScheduleCreateVO(
        @Schema(description = "排期ID")
        Long id,

        @Schema(description = "投递记录ID")
        Long applicationId,

        @Schema(description = "排期状态", example = "PENDING_CONFIRMATION")
        String status,

        @Schema(description = "乐观锁版本号")
        Integer version,

        @Schema(description = "面试时间")
        OffsetDateTime interviewTime,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
