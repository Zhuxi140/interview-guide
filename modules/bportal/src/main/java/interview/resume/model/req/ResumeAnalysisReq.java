package interview.resume.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 触发简历 AI 分析请求
 */
@Data
@Schema(description = "触发简历 AI 分析请求")
public class ResumeAnalysisReq {
    @Schema(description = "简历ID")
    @NotNull(message = "简历 ID 不能为空")
    private Long resumeId;
}
