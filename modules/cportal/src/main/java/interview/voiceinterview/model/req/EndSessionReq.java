package interview.voiceinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 结束语音会话请求
 * @since 2026/7/26 14:05
 */
@Data
@Schema(description = "结束语音会话请求")
public class EndSessionReq {

    @NotBlank(message = "期望会话状态不能为空")
    @Schema(description = "期望会话状态（乐观锁）", example = "IN_PROGRESS")
    private String expectedStatus;
}
