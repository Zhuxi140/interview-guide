package interview.matching.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 简历投递请求（jobId 取自路径参数）
 */
@Data
@Schema(description = "简历投递请求")
public class JobApplicationSubmitReq {
    @Schema(description = "简历ID")
    @NotNull(message = "简历 ID 不能为空")
    private Long resumeId;
}
