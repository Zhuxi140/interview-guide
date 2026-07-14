package interview.matching.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 简历投递请求（jobId 取自路径参数）
 */
@Data
public class JobApplicationSubmitReq {

    @NotNull(message = "简历 ID 不能为空")
    private Long resumeId;
}
