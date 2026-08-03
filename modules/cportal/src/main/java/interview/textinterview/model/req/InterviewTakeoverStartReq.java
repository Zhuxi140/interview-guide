package interview.textinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "面试官开始人工接管请求")
public record InterviewTakeoverStartReq(
        @Size(max = 256, message = "接管原因长度不能超过256个字符")
        @Schema(description = "可选的接管原因", example = "需要进一步确认项目经历")
        String reason
) {
}
