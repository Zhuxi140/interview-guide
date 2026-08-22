package interview.tutor.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建 AI 答疑会话请求。
 */
@Data
@Schema(description = "创建 AI 答疑会话请求")
public class TutorSessionCreateReq {

    @NotNull(message = "关联面评报告 ID 不能为空")
    @Schema(description = "本人已生成完成的面评报告 ID", example = "193882715")
    private Long associatedReportId;

    @Size(max = 128, message = "会话标题最长 128 位")
    @Schema(description = "会话自拟标题，不提交时由服务端生成默认标题",
            example = "Java 基础复盘答疑")
    private String sessionTitle;
}
