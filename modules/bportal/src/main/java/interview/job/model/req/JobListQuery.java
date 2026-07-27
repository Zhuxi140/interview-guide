package interview.job.model.req;

import interview.job.model.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 岗位列表查询参数
 */
@Data
@Schema(description = "岗位列表查询参数")
public class JobListQuery {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数")
    private Integer size = 20;

    @Schema(description = "岗位状态筛选")
    private JobStatus status;

    @Schema(description = "搜索关键词")
    private String keyword;

    @Pattern(regexp = "^createdAt$", message = "仅支持按 createdAt 排序")
    @Schema(description = "排序字段", example = "createdAt")
    private String sort = "createdAt";

    @Pattern(regexp = "^(?i)(asc|desc)$", message = "排序方向仅支持 asc / desc")
    @Schema(description = "排序方向", example = "desc")
    private String order = "desc";
}
