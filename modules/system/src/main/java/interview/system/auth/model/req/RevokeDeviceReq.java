package interview.system.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 下线请求
 */

@Schema(description = "下线请求")
@Data
public class RevokeDeviceReq {
    @Schema(description = "手机验证码")
    @NotBlank(message = "手机验证码不能为空")
    private final String code;
}
