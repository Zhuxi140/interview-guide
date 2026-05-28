package interview.system.auth;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.Enum.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.auth.model.entity.UserToken;
import interview.system.auth.model.req.RegisterReq;
import interview.system.rbac.model.entity.SysUser;
import interview.system.rbac.service.UsersService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zhuxi
 * @since 2026/5/27 14:05
 * @apiNote 认证服务实现
 */

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl extends ServiceImpl<AuthMapper, UserToken> implements AuthService{

    private final UsersService usersService;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public UserToken register(RegisterReq register) {
        // TODO:查库确认用户是否存在
        boolean isExists = usersService.lambdaQuery()
                .eq(SysUser::getUsername, register.getUsername())
                .exists();

        if (isExists) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        //TODO: 效验邮箱是否存在
        if (StrUtil.isNotBlank(register.getEmail())) {
            isExists = usersService.lambdaQuery()
                    .eq(SysUser::getEmail, register.getEmail())
                    .exists();
            if (isExists) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        // TODO: 效验手机号是否已存在
        if (StrUtil.isNotBlank(register.getPhone())) {
            isExists = usersService.lambdaQuery()
                    .eq(SysUser::getPhone, register.getPhone())
                    .exists();
            if (isExists) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }
        SysUser sysUser = new SysUser();
        // TODO: 设置用户信息
        // TODO: 密码加密
        usersService.save(sysUser);

        // TODO: 生成用户Token
        UserToken userToken = new UserToken();
        save(userToken);
        return userToken;
    }

    @Override
    public UserToken login(String username, String password) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public UserToken refreshToken(String refreshToken) {
        return null;
    }

    @Override
    public void logout(String refreshToken) {
        // TODO: 集成Redis实现
    }

    @Override
    public UserToken getUserToken() {
        return null;
    }
}
