package interview.system.auth.req;

import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 刷新 Token 请求
 * @since 2026/5/27 14:05
 */
@Data
public class RefreshTokenReq {

    private String refreshToken;
}
