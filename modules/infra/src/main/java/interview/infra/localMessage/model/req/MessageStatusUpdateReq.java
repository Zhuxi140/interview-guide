package interview.infra.localMessage.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 手动设置消息状态请求
 * @since 2026/07/18
 */
@Data
@Schema(description = "手动设置消息状态请求")
public class MessageStatusUpdateReq {
    @Schema(description = "目标状态：SUCCESS / IGNORED / FAILED")
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "SUCCESS|IGNORED|FAILED", message = "状态仅支持 SUCCESS / IGNORED / FAILED")
    private String status;
}
