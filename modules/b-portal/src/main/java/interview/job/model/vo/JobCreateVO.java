package interview.job.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.job.model.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 发布岗位响应
 */
@Builder
@Schema(description = "发布岗位响应")
public record JobCreateVO(
        @Schema(description = "岗位 ID")
        Long id,
        @Schema(description = "岗位名称")
        String title,
        @Schema(description = "岗位状态")
        JobStatus status,
        @Schema(description = "发布时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
