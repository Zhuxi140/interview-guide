package interview.code.model.req;

import interview.code.model.enums.CodeExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 企业侧候选人提交记录分页查询参数。
 */
@Data
@Schema(description = "企业侧候选人提交记录分页查询参数")
public class EnterpriseCodeSubmissionSearchReq {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "按面试会话过滤", example = "1234567890")
    private Long sessionId;

    @Schema(description = "按题目过滤", example = "1234567890")
    private Long questionId;

    @Schema(description = "按执行状态过滤", example = "PASS",
            allowableValues = {"QUEUED", "RUNNING", "PASS", "FAIL", "TIMEOUT", "ERROR"})
    private CodeExecutionStatus executionStatus;
}
