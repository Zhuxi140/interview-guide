package interview.system.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 登出请求
 */
@Data
@Schema(description = "登出请求")
public class LogoutReq {

    @NotBlank(message = "Refresh Token 不能为空")
    @Schema(description = "待撤销的长时 Refresh Token")
    private String refreshToken;
}
