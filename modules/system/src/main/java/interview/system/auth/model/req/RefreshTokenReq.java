package interview.system.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 刷新 Token 请求
 */
@Data
@Schema(description = "刷新 Token 请求")
public class RefreshTokenReq {

    @NotBlank(message = "Refresh Token 不能为空")
    @Schema(description = "长时 Refresh Token")
    private String refreshToken;

    @Positive(message = "企业 ID 必须为正数")
    @Schema(description = "当前企业工作区 ID；个人或平台工作区不传")
    private Long enterpriseId;
}
