package interview.matching.model.req;

import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 投递记录分页查询参数。
 */
@Data
@Schema(description = "投递记录分页查询参数")
public class JobApplicationPageReq {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "投递状态")
    private JobApplicationStatus status;

    @Pattern(regexp = "^createdAt$", message = "仅支持按 createdAt 排序")
    @Schema(description = "排序字段", example = "createdAt")
    private String sort = "createdAt";

    @Pattern(regexp = "^(?i)(asc|desc)$", message = "排序方向仅支持 asc / desc")
    @Schema(description = "排序方向", example = "desc")
    private String order = "desc";
}
