package interview.matching.model.req;

import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * HR 审核 AI 初筛建议请求。
 */
@Data
@Schema(description = "HR 审核 AI 初筛建议请求")
public class ApplicationAiReviewReq {

    @NotNull(message = "期望投递状态不能为空")
    @Schema(description = "客户端读取到的投递状态", example = "REVIEWING")
    private JobApplicationStatus expectedApplicationStatus;

    @NotNull(message = "审核决定不能为空")
    @Schema(description = "HR 最终决定：PASSED 或 REJECTED", example = "PASSED")
    private JobApplicationStatus decision;
}
