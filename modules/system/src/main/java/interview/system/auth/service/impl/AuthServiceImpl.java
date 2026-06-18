package interview.system.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.id.NanoId;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.*;
import interview.common.exception.BusinessException;
import interview.common.util.CryptoUtil;
import interview.common.util.JwttUtil;
import interview.framework.config.properties.JwtProperties;
import interview.framework.context.AuthContext;
import interview.system.auth.AuthKeyConstant;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.model.enums.SmsType;
import interview.system.auth.mapper.AuthMapper;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.entity.UserToken;
import interview.system.auth.model.req.*;
import interview.system.auth.model.vo.RefreshTokenVO;
import interview.system.auth.model.vo.TokenInfoVO;
import interview.system.auth.service.AuthService;
import interview.system.auth.service.SmsService;
import interview.system.auth.service.UsersService;
import interview.system.rbac.mapper.PermissionsMapper;
import interview.system.rbac.model.entity.SysRole;
import interview.system.auth.model.entity.SysUser;
import interview.system.rbac.model.entity.SysUserRole;
import interview.system.auth.model.enums.UserStatus;
import interview.system.rbac.service.*;
import interview.system.tenant.EnterpriseTeamMembersService;
import interview.system.tenant.EnterprisesService;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @author zhuxi
 * @apiNote 认证服务实现
 */

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl extends ServiceImpl<AuthMapper, UserToken> implements AuthService {

    private final SmsService smsService;
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

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public RegisterBo register(RegisterReq register) {

        // 验证code 并 删除code
        smsService.verifyCode(register.getPhone(), register.getCode(), SmsType.REGISTER);

        // 验证用户名、手机号、邮箱是否已存在
        checkUnique( register);

        // 构建用户
        Long number = (Long)customIdGenerator.nextId(null);
        SysUser user = buildUser(register, number);
        usersService.save(user);

        // FK检查
        Integer code = Role.CANDIDATE.getCode();
        if (!rolesService.lambdaQuery()
                .eq(SysRole::getRoleCode, code)
                .exists()) {
            log.error("外键拦截: 角色:[{}]在Role表内已不存在",Role.CANDIDATE.getCode());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        // 构建角色(默认求职者)
        SysUserRole sysUserRole = new SysUserRole();
        sysUserRole.setUserId(user.getId());
        sysUserRole.setRoleId(code.longValue());
        userRolesService.save(sysUserRole);


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

        Long userId = user.getId();
        // 构建Token 并 去除本设备的旧Token
        String raw = checkAndGetUserToken(userId, login.getDeviceInfo(), login.getIpAddress());

        // 加载企业信息 和 角色权限信息
        EnterpriseContext enterpriseContext = loadEnterpriseContext(userId);
        RbacContext rbacContext = loadRbacContext(userId, enterpriseContext.enterpriseId);

        List<String> roleCodes = rbacContext.roleCodes;

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType().name());
        claims.put("riskLevel", user.getRiskLevel().getCode());
        claims.put("roleScope", roleCodes);
        Long enterpriseId = enterpriseContext.enterpriseId;
        if (enterpriseId != null) {
            claims.put("enterpriseId", enterpriseId);
        }
        String token = jwttUtil.generatorToken(claims, userId);

        return LoginBO.builder()
                    .userId(userId)
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
    public RefreshTokenVO refreshToken(RefreshTokenReq refresh) {
        String refreshToken   = refresh.getRefreshToken();
        // 验证refreshToken是否存在
        UserToken token = lambdaQuery()
                .select(
                        UserToken::getUserId,
                        UserToken::getDeviceInfo,
                        UserToken::getIpAddress,
                        UserToken::getIsRevoked
                        )
                .eq(UserToken::getRefreshTokenHash, DigestUtil.sha256Hex(refreshToken))
                .gt(UserToken::getExpiresAt, OffsetDateTime.now())
                .one();

        if (token == null) {
            log.error("异常的登录状态,refreshToken：[{}]不存在或过期", refreshToken);
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        Long userId = token.getUserId();
        String ipAddress = token.getIpAddress();

        //FK检查
        if (!usersService.lambdaQuery()
                .eq(SysUser::getId,userId)
                .exists()) {
            log.error("外键拦截: 外键UserId：[{}]在User表已不存在",userId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        // 触发重放检测
        if (token.getIsRevoked()){
            log.error("检测到过期Refresh Token 再次被使用! userId:[{}], IP:[{}]", userId, ipAddress);


            lambdaUpdate()
                    .eq(UserToken::getUserId, userId)
                    .set(UserToken::getIsRevoked, true)
                    .update();

            throw new BusinessException(ErrorCode.RISK_CONTROL);
        }

        // 生成refreshToken
        String raw = checkAndGetUserToken(userId, token.getDeviceInfo(), ipAddress);

        // 验证accessToken
        String access = refresh.getAccessToken();
        Claims claims;
        try{
            claims = jwttUtil.parseToken(access);
        }catch (ExpiredJwtException e){
            claims = e.getClaims();
        }catch (Exception e){
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        long expire = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (expire > 0) {
            // 如果accessToken未过期，则加入黑名单
            stringRedisTemplate.opsForValue().set(
                    AuthKeyConstant.getTokenBanKey(access),
                    "1",
                    expire,
                    TimeUnit.MILLISECONDS
            );
        }

        // 生成新的accessToken
        String accessToken = jwttUtil.generatorToken(claims,token.getUserId());

        return RefreshTokenVO.builder()
                    .accessToken(accessToken)
                    .refreshToken(raw)
                    .expiresIn(jwtProperties.getExpiration() * 60)
                    .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void logout(LogoutReq logout) {

        String accessToken = logout.getAccessToken();
        try{
            Claims claims = jwttUtil.parseToken(accessToken);
            Date expiration = claims.getExpiration();
            long expire = expiration.getTime() - System.currentTimeMillis();

            if (expire > 0){
                stringRedisTemplate.opsForValue()
                        .set(AuthKeyConstant.getTokenBanKey(accessToken), "1", expire, TimeUnit.MILLISECONDS);
            }
        }catch (ExpiredJwtException e){
            log.info("Logout: AccessToken 已过期，无需加入黑名单");
        }

        String hashToken = DigestUtil.sha256Hex(logout.getRefreshToken());
        lambdaUpdate()
                    .eq(UserToken::getRefreshTokenHash, hashToken)
                    .set(UserToken::getIsRevoked, true)
                    .update();
    }

    @Override
    public List<TokenInfoVO> getUserToken() {

        List<UserToken> list = lambdaQuery()
                .select(
                        UserToken::getId,
                        UserToken::getDeviceInfo,
                        UserToken::getIpAddress,
                        UserToken::getExpiresAt,
                        UserToken::getCreatedAt
                )
                .eq(UserToken::getUserId, AuthContext.getUserId())
                .eq(UserToken::getIsRevoked, false)
                .gt(UserToken::getExpiresAt, OffsetDateTime.now())
                .list();

        return list.stream()
                .map(item -> TokenInfoVO.builder()
                        .tokenId(item.getId())
                        .deviceInfo(item.getDeviceInfo())
                        .ipAddress(item.getIpAddress())
                        .createdAt(item.getCreatedAt())
                        .expiresAt(item.getExpiresAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void revoke(Long tokenId, RevokeDeviceReq code) {

        Long userId = AuthContext.getUserId();

        SysUser user = usersService.lambdaQuery()
                .select(SysUser::getPhone)
                .eq(SysUser::getId, userId)
                .one();
        if (user == null){
            log.error("token有效，但数据库无此实体，userId：[{}]", userId);
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        smsService.verifyCode(user.getPhone(), code.getCode(), SmsType.SENSITIVE_OPERATION);

        boolean update = lambdaUpdate()
                .eq(UserToken::getId, tokenId)
                .eq(UserToken::getUserId, userId)
                .set(UserToken::getIsRevoked, true)
                .update();

        if (!update){
            log.warn("越权拦截或数据不存在: 用户 [{}] 试图作废 Token [{}]", userId, tokenId);
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        log.info("设备下线成功: 用户 [{}] 成功移除了设备 Token [{}]", userId, tokenId);
    }

    @Override
    public UserInfoBO getUserInfo() {

        Long userId = AuthContext.getUserId();
        SysUser user = usersService.lambdaQuery()
                .select(
                    SysUser::getUsername,
                    SysUser::getNickname,
                    SysUser::getPhone,
                    SysUser::getEmail,
                    SysUser::getAvatarUrl,
                    SysUser::getStatus
                )
                .eq(SysUser::getId, AuthContext.getUserId())
                .one();

        if (user == null){
            log.error("token有效，但数据库无此实体，userId：[{}]", userId);
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        if (user.getStatus() != UserStatus.NORMAL){
            log.error("用户已冻结，userId:[{}], userStatus:[{}]", userId, user.getStatus().name());
            throw new BusinessException(ErrorCode.USER_ALREADY_FREEZE);
        }

        return UserInfoBO.builder()
                .id(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .userType(AuthContext.getUserType().name())
                .status(user.getStatus())
                .build();
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
                .username(user.getUsername())
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

    private String checkAndGetUserToken(Long userId,String deviceInfo,String ipAddress){
            String raw = IdUtil.fastSimpleUUID();
            UserToken userToken = UserToken.builder()
                        .userId(userId)
                        .refreshTokenHash(DigestUtil.sha256Hex(raw))
                        .deviceInfo(deviceInfo)
                        .ipAddress(ipAddress)
                        .expiresAt(OffsetDateTime.now().plusDays(7))
                        .build();

                lambdaUpdate()
                    .eq(UserToken::getUserId, userId)
                    .eq(UserToken::getDeviceInfo, deviceInfo)
                    .eq(UserToken::getIsRevoked,false)
                    .set(UserToken::getIsRevoked, true)
                    .update();

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

    private RbacContext loadRbacContext(Long userId,Long enterpriseId) {
        ArrayList<Long> roleIds = userRolesService.lambdaQuery()
                .select(SysUserRole::getRoleId)
                .eq(SysUserRole::getUserId, userId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toCollection(ArrayList::new));
        if (enterpriseId != null){
            List<Long> enterpriseRoleIds = enterpriseTeamMembersService.lambdaQuery()
                    .select(EnterpriseTeamMember::getRoleId)
                    .eq(EnterpriseTeamMember::getUserId, userId)
                    .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                    .list()
                    .stream()
                    .map(EnterpriseTeamMember::getRoleId)
                    .toList();

            roleIds.addAll(enterpriseRoleIds);
        }


        if (CollUtil.isEmpty(roleIds)) {
            log.error("脏数据拦截: 用户ID [{}], 企业Id [{}], 其关联的角色ID 在 user_roles 和 enterpiseTeamMeber 表中查不到实体记录！", userId, enterpriseId);
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
