package interview.job.model.req;

import interview.job.model.enums.JobStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 开关岗位请求
 */
@Data
public class JobStatusReq {

    @NotNull(message = "岗位状态不能为空")
    @Min(value = 0, message = "岗位状态不合法")
    @Max(value = 1, message = "岗位状态不合法")
    private JobStatus status;
}
