package interview.system.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 刷新 Token 请求
 * @since 2026/5/27 14:05
 */
@Data
@Schema(description = "刷新 Token 请求")
public class RefreshTokenReq {

    @NotBlank(message = "Refresh Token 不能为空")
    @Schema(description = "长时 Refresh Token")
    private String refreshToken;
}
