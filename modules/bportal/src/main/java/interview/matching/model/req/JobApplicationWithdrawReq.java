package interview.matching.model.req;

import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 候选人撤回投递请求。
 */
@Data
@Schema(description = "候选人撤回投递请求")
public class JobApplicationWithdrawReq {

    @NotNull(message = "期望当前状态不能为空")
    @Schema(description = "客户端读取记录时看到的当前状态，仅允许 APPLIED / REVIEWING")
    private JobApplicationStatus expectedStatus;
}
