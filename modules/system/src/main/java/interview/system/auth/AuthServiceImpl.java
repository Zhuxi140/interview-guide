package interview.system.auth;

import cn.hutool.core.lang.id.NanoId;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.common.util.CryptoUtil;
import interview.common.util.JwttUtil;
import interview.framework.config.properties.JwtProperties;
import interview.system.auth.enums.SmsType;
import interview.system.auth.mapper.AuthMapper;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.entity.UserToken;
import interview.system.auth.model.req.RegisterReq;
import interview.system.rbac.model.entity.SysUser;
import interview.system.rbac.service.UsersService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

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
    private final StringRedisTemplate stringRedisTemplate;
    private final JwttUtil jwttUtil;
    private final IdentifierGenerator customIdGenerator;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public RegisterBo register(RegisterReq register) {

        System.out.println(register.toString());

        // 验证验证码
        String code = stringRedisTemplate.opsForValue().get(AuthKeyConstant.getSmsCodeKey(register.getPhone(), SmsType.REGISTER));
        Optional.ofNullable(code)
                .filter(s -> s.equals(register.getCode()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_ERROR_OR_EXPIRED));

        // 删除验证码
        stringRedisTemplate.delete(AuthKeyConstant.getSmsCodeKey(register.getPhone(), SmsType.REGISTER));


        //查库确认用户名是否存在
        boolean isExists = usersService.lambdaQuery()
                .eq(SysUser::getUsername, register.getUsername())
                .exists();

        if (isExists) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        //效验手机号是否已存在
        isExists = usersService.lambdaQuery()
                .eq(SysUser::getPhone, register.getPhone())
                .exists();
        if (isExists) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        //验邮箱是否存在
        if (StrUtil.isNotBlank(register.getEmail())) {
            isExists = usersService.lambdaQuery()
                    .eq(SysUser::getEmail, register.getEmail())
                    .exists();
            if (isExists) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        Long number = (Long)customIdGenerator.nextId(null);

        SysUser user = SysUser.builder()
                .id(number)
                .username(register.getUsername())
                .email(register.getEmail())
                .phone(register.getPhone())
                .passwordHash(CryptoUtil.hashPassword(register.getPassword()))
                .nickname("用户" + NanoId.randomNanoId(8))
                .userType(UserType.fromString(register.getUserType()))
                .build();

        usersService.save(user);

        String raw = IdUtil.fastSimpleUUID();
        String hashToken = DigestUtil.sha256Hex(raw);
        UserToken userToken = UserToken.builder()
                .userId(number)
                .refreshTokenHash(hashToken)
                //TODO: 获取设备信息 和 IP地址
                .deviceInfo(null)
                .ipAddress(null)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();

        save(userToken);

        // 生成JWT令牌
        String jwtToken = jwttUtil.generatorToken(Map.of(
                "userId", number,
                "username", user.getUsername(),
                "userType", user.getUserType().name()
        ), number);

        return RegisterBo.builder()
                .userId(number)
                .accessToken(jwtToken)
                .refreshToken(raw)
                .expiresIn(jwtProperties.getExpiration() * 60L)
                .build();
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
