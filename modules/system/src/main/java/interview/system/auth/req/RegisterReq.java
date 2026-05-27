package interview.system.auth.req;

import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 注册请求
 * @since 2026/5/27 14:05
 */
@Data
public class RegisterReq {

    private String username;
    private String password;
    private String email;
    private String phone;
    private String userType;
}
