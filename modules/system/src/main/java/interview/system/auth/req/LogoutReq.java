package interview.system.auth.req;

import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 登出请求
 * @since 2026/5/27 14:05
 */
@Data
public class LogoutReq {

    private String refreshToken;
}
