package interview.job.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.job.model.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 岗位列表项响应
 */
@Builder
@Schema(description = "岗位列表项响应")
public record JobListItemVO(
        @Schema(description = "岗位 ID")
        Long id,
        @Schema(description = "岗位名称")
        String title,
        @Schema(description = "所属部门")
        String department,
        @Schema(description = "工作地点")
        String location,
        @Schema(description = "岗位状态")
        JobStatus status,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @Schema(description = "候选人数量")
        Integer candidateCount
) {
}
