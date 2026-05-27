package interview.system.auth.req;

import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 登录请求
 * @since 2026/5/27 14:05
 */
@Data
public class LoginReq {

    private String username;
    private String password;
    private String deviceInfo;
    private String ipAddress;
}
