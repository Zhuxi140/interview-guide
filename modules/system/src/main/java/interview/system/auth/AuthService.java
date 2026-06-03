package interview.system.auth;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.system.auth.enums.SmsType;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.entity.UserToken;
import interview.system.auth.model.req.RegisterReq;

/**
 * @author zhuxi
 * @apiNote 认证服务
 * @since 2026/5/27 14:05
 */
public interface AuthService extends IService<UserToken> {

    /**
     *  注册预
     * @param register 注册信息
     * @return 注册结果
     */
    RegisterBo register(RegisterReq register);

    UserToken login(String username,String password);

    UserToken refreshToken(String refreshToken);

    void logout(String refreshToken);

    UserToken getUserToken();


}
