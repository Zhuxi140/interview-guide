package interview.system.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 登录请求
 * @since 2026/5/27 14:05
 */
@Data
@Schema(description = "登录请求")
public class LoginReq {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "登录账号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;

    @Schema(description = "设备 UA")
    private String deviceInfo;

    @Schema(description = "登录 IP（后端可自动获取）")
    private String ipAddress;
}
