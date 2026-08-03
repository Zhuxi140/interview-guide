package interview.textinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "候选人可面试时间完整更新请求")
public record CandidateInterviewAvailabilityUpdateReq(
        @NotNull(message = "乐观锁版本号不能为空")
        @Min(value = 0, message = "乐观锁版本号不能小于0")
        @Schema(description = "客户端读取到的版本号", example = "0")
        Integer expectedVersion,

        @NotBlank(message = "时区不能为空")
        @Size(max = 64, message = "时区长度不能超过64个字符")
        @Schema(description = "IANA 时区", example = "Asia/Shanghai")
        String timezone,

        @NotNull(message = "可面试时间范围不能为空")
        @Size(max = 50, message = "可面试时间范围不能超过50组")
        @Valid
        @Schema(description = "完整的可面试时间范围；空数组表示清空")
        List<Range> ranges
) {

    @Schema(description = "单个可面试时间范围")
    public record Range(
            @NotNull(message = "开始时间不能为空")
            @Schema(description = "开始时间", example = "2026-08-05T10:00:00+08:00")
            OffsetDateTime startTime,

            @NotNull(message = "结束时间不能为空")
            @Schema(description = "结束时间", example = "2026-08-05T12:00:00+08:00")
            OffsetDateTime endTime
    ) {
    }
}
