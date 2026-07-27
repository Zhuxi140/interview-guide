package interview.job.model.req;

import interview.job.model.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 开关岗位请求
 */
@Data
@Schema(description = "开关岗位请求")
public class JobStatusReq {

    @NotNull(message = "岗位状态不能为空")
    @Schema(description = "目标状态")
    private JobStatus status;

    @NotNull(message = "期望版本不能为空")
    @Min(value = 0, message = "期望版本不能小于 0")
    @Schema(description = "客户端读取岗位时得到的版本号", example = "0")
    private Integer expectedVersion;
}
