package interview.matching.model.req;

import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 投递状态变更请求（applicationId 取自路径参数）
 */
@Data
@Schema(description = "投递状态变更请求")
public class JobApplicationStatusReq {
    @Schema(description = "目标状态")
    @NotNull(message = "目标状态不能为空")
    private JobApplicationStatus status;
}
