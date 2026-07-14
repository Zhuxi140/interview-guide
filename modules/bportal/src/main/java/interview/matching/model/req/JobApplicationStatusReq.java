package interview.matching.model.req;

import interview.matching.model.enums.JobApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 投递状态变更请求（applicationId 取自路径参数）
 */
@Data
public class JobApplicationStatusReq {
    @NotNull(message = "目标状态不能为空")
    private JobApplicationStatus status;
}
