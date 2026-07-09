package interview.job.model.req;

import interview.job.model.enums.JobStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 岗位列表查询参数
 */
@Data
public class JobListQuery {

    @Min(value = 1, message = "页码最小为 1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private Integer size = 20;

    private JobStatus status;

    private String keyword;
}
