package interview.system.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.id.NanoId;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.enums.RiskLevel;
import interview.common.enums.RoleScope;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.common.util.CryptoUtil;
import interview.common.util.JwttUtil;
import interview.framework.config.properties.JwtProperties;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.enums.SmsType;
import interview.system.auth.mapper.AuthMapper;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.entity.UserToken;
import interview.system.auth.model.req.LoginReq;
import interview.system.auth.model.req.LogoutReq;
import interview.system.auth.model.req.RegisterReq;
import interview.system.rbac.mapper.PermissionsMapper;
import interview.system.rbac.model.entity.SysRole;
import interview.system.rbac.model.entity.SysUser;
import interview.system.rbac.model.entity.SysUserRole;
import interview.system.rbac.model.enums.UserStatus;
import interview.system.rbac.service.*;
import interview.system.tenant.EnterpriseTeamMembersService;
import interview.system.tenant.EnterprisesService;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


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
    private final EnterprisesService enterprisesService;
    private final UserRolesService userRolesService;
    private final RolesService rolesService;
    private final PermissionsMapper permissionsMapper;
    private final EnterpriseTeamMembersService enterpriseTeamMembersService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwttUtil jwttUtil;
    private final IdentifierGenerator customIdGenerator;
    private final JwtProperties jwtProperties;
    private static final DefaultRedisScript<Long> CHECK_DELETE_CODE_SCRIPT;

    static{
        CHECK_DELETE_CODE_SCRIPT = new DefaultRedisScript<>();
        CHECK_DELETE_CODE_SCRIPT.setScriptText(ResourceUtil.readUtf8Str("GetDeleteCode.lua"));
        CHECK_DELETE_CODE_SCRIPT.setResultType(Long.class);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public RegisterBo register(RegisterReq register) {

        // 验证code 并 删除code
        checkCode( register);

        // 验证用户名、手机号、邮箱是否已存在
        checkUnique( register);

        // 构建用户
        Long number = (Long)customIdGenerator.nextId(null);
        SysUser user = buildUser(register, number);
        usersService.save(user);

        // TODO: 构建角色及权限

        // 构建Token
        String raw = IdUtil.fastSimpleUUID();
        UserToken userToken = buildUserToken(raw, number);
        save(userToken);

        return buildRegisterBo(raw, user, number);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public LoginBO login(LoginReq login) {

        // 验证用户名、状态、密码
        SysUser user = checkAndGetUser(login);

        // 构建Token 并 去除本设备的旧Token
        String raw = checkAndGetUserToken(login, user);

        // 加载企业信息 和 角色权限信息
        EnterpriseContext enterpriseContext = loadEnterpriseContext(user.getId());
        RbacContext rbacContext = loadRbacContext(user.getId());

        List<String> roleCodes = rbacContext.roleCodes;

        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType().name());
        claims.put("riskLevel", user.getRiskLevel().getCode());
        claims.put("roleScope", roleCodes);
        Long enterpriseId = enterpriseContext.enterpriseId;
        if (enterpriseId != null) {
            claims.put("enterpriseId", enterpriseId);
        }
        String token = jwttUtil.generatorToken(claims, user.getId());

        return LoginBO.builder()
                    .userId(user.getId())
                    .enterpriseId(enterpriseId)
                    .enterpriseName(enterpriseContext.name)
                    .logoUrl(enterpriseContext.logoUrl)
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .userType(user.getUserType().name())
                    .roles(roleCodes)
                    .permissions(rbacContext.permissions)
                    .accessToken(token)
                    .refreshToken(raw)
                    .expiresIn(jwtProperties.getExpiration() * 60)
                    .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public UserToken refreshToken(String refreshToken) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void logout(LogoutReq logout) {

        Claims claims = jwttUtil.parseToken(logout.getAccessToken());
        Date expiration = claims.getExpiration();

        long expire = expiration.getTime() - System.currentTimeMillis();

        if (expire > 0){
            stringRedisTemplate.opsForValue()
                    .set(AuthKeyConstant.getTokenBanKey(logout.getAccessToken()), "1", expire, TimeUnit.MILLISECONDS);
        }

        String hashToken = DigestUtil.sha256Hex(logout.getRefreshToken());
        lambdaUpdate()
                    .eq(UserToken::getRefreshTokenHash, hashToken)
                    .remove();
    }

    @Override
    public UserToken getUserToken() {
        return null;
    }


    private void checkUnique(RegisterReq register){
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
    }

    private SysUser buildUser(RegisterReq register,Long number){
        return SysUser.builder()
                .id(number)
                .username(register.getUsername())
                .email(register.getEmail())
                .phone(register.getPhone())
                .passwordHash(CryptoUtil.hashPassword(register.getPassword()))
                .nickname("用户" + NanoId.randomNanoId(8))
                .userType(UserType.fromString(register.getUserType()))
                .build();
    }

    private UserToken buildUserToken(String raw, Long number){
        String hashToken = DigestUtil.sha256Hex(raw);
        return UserToken.builder()
                .userId(number)
                .refreshTokenHash(hashToken)
                //TODO: 获取设备信息 和 IP地址
                .deviceInfo(null)
                .ipAddress(null)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
    }

    private void checkCode(RegisterReq register){
        // 验证验证码 并删除验证码
        Long result = stringRedisTemplate.execute(
                CHECK_DELETE_CODE_SCRIPT,
                Collections.singletonList(AuthKeyConstant.getSmsCodeKey(register.getPhone(), SmsType.REGISTER)),
                register.getCode());

        if(result == null || result == 0L){
            throw new BusinessException(ErrorCode.CODE_ERROR_OR_EXPIRED);
        }else if(result == 1L){
            throw new BusinessException(ErrorCode.CODE_ERROR);
        }
    }

    private RegisterBo buildRegisterBo(String raw,SysUser user,Long number){
        // 生成JWT令牌
        String jwtToken = jwttUtil.generatorToken(Map.of(
                "userId", number,
                "username", user.getUsername(),
                "userType", user.getUserType().name(),
                "riskLevel", RiskLevel.NO_RISK.getCode()
        ), number);

        return RegisterBo.builder()
                .userId(number)
                .accessToken(jwtToken)
                .refreshToken(raw)
                .expiresIn(jwtProperties.getExpiration() * 60L)
                .build();
    }

    private SysUser checkAndGetUser(LoginReq login){
        SysUser user = usersService.lambdaQuery()
                .select(
                        SysUser::getId,
                        SysUser::getUsername,
                        SysUser::getPhone,
                        SysUser::getEmail,
                        SysUser::getPasswordHash,
                        SysUser::getNickname,
                        SysUser::getUserType,
                        SysUser::getRiskLevel,
                        SysUser::getStatus
                )
                .eq(SysUser::getUsername, login.getUsername())
                .one();

        if (user == null){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getStatus() != UserStatus.NORMAL){
            throw new BusinessException(ErrorCode.USER_ALREADY_FREEZE);
        }

        if (!CryptoUtil.checkPassword(login.getPassword(), user.getPasswordHash())){
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        return user;
    }

    private String checkAndGetUserToken(LoginReq login,SysUser user){
            String raw = IdUtil.fastSimpleUUID();
            UserToken userToken = UserToken.builder()
                        .userId(user.getId())
                        .refreshTokenHash(DigestUtil.sha256Hex(raw))
                        .deviceInfo(login.getDeviceInfo())
                        .ipAddress(login.getIpAddress())
                        .expiresAt(OffsetDateTime.now().plusDays(7))
                        .build();

            lambdaUpdate()
                .eq(UserToken::getUserId, user.getId())
                .eq(UserToken::getDeviceInfo, login.getDeviceInfo())
                .remove();
            save(userToken);
        return raw;
    }

    private EnterpriseContext loadEnterpriseContext(Long userId) {
        Long enterpriseId = enterpriseTeamMembersService.lambdaQuery()
                .select(EnterpriseTeamMember::getEnterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .oneOpt()
                .map(EnterpriseTeamMember::getEnterpriseId)
                .orElse(null);

        if (enterpriseId == null) {
            return new EnterpriseContext(null, null, null);
        }

        Enterprise enterprise = enterprisesService.lambdaQuery()
                .select(Enterprise::getName, Enterprise::getLogoUrl)
                .eq(Enterprise::getId, enterpriseId)
                .one();

        if (enterprise == null) {
            log.error("脏数据拦截: 用户ID [{}], 其关联的企业ID [{}] 在 enterprises 表中查不到实体记录！", userId, enterpriseId);
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }
        return new EnterpriseContext(enterpriseId, enterprise.getName(), enterprise.getLogoUrl());
    }

    private RbacContext loadRbacContext(Long userId) {
        List<Long> roleIds = userRolesService.lambdaQuery()
                .select(SysUserRole::getRoleId)
                .eq(SysUserRole::getUserId, userId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();

        if (CollUtil.isEmpty(roleIds)) {
            log.error("脏数据拦截: 用户ID [{}], 其关联的角色ID 在 user_roles 表中查不到实体记录！", userId);
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        List<SysRole> sysRoles = rolesService.lambdaQuery()
                .select(SysRole::getRoleCode, SysRole::getRoleScope)
                .in(SysRole::getId, roleIds)
                .list();

        List<String> roleCodes = sysRoles.stream().map(SysRole::getRoleCode).toList();
        List<String> roleScopes = sysRoles.stream().map(SysRole::getRoleScope).map(RoleScope::name).toList();
        List<String> permissions = permissionsMapper.getPermCodeByRoleId(roleIds);

        return new RbacContext(roleCodes, roleScopes, permissions);
    }

    // 企业上下文载体
    private record EnterpriseContext(Long enterpriseId, String name, String logoUrl) {}

    // 权限上下文载体
    private record RbacContext(List<String> roleCodes, List<String> roleScopes, List<String> permissions) {}
}
