package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "候选人可面试时间")
public record CandidateInterviewAvailabilityVO(
        @Schema(description = "IANA 时区", example = "Asia/Shanghai")
        String timezone,

        @Schema(description = "可面试时间范围")
        List<Range> ranges,

        @Schema(description = "乐观锁版本号", example = "0")
        Integer version,

        @Schema(description = "最后更新时间", example = "2026-08-02T10:00:00+08:00", nullable = true)
        OffsetDateTime updatedAt
) {

    @Schema(description = "单个可面试时间范围")
    public record Range(
            @Schema(description = "开始时间", example = "2026-08-05T10:00:00+08:00")
            OffsetDateTime startTime,

            @Schema(description = "结束时间", example = "2026-08-05T12:00:00+08:00")
            OffsetDateTime endTime
    ) {
    }
}
