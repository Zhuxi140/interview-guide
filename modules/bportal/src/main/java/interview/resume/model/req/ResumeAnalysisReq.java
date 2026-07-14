package interview.resume.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 触发简历 AI 分析请求
 */
@Data
public class ResumeAnalysisReq {

    @NotNull(message = "简历 ID 不能为空")
    private Long resumeId;
}
