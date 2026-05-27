package interview.system.auth;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.system.auth.req.RegisterReq;
import org.springframework.stereotype.Service;

/**
 * @author zhuxi
 * @since 2026/5/27 14:05
 * @apiNote 认证服务实现
 */

@Service
public class AuthServiceImpl extends ServiceImpl<AuthMapper,UserToken> implements AuthService{


    @Override
    public UserToken register(RegisterReq register) {
        return null;
    }

    @Override
    public UserToken login(String username, String password) {
        return null;
    }

    @Override
    public UserToken refreshToken(String refreshToken) {
        return null;
    }

    @Override
    public void logout(String refreshToken) {

    }

    @Override
    public UserToken getUserToken() {
        return null;
    }
}
