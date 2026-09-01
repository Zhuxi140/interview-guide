package interview.system.auth.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.id.NanoId;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.dto.SecureChallengeStartDTO;
import interview.common.constant.SecureActionContext;
import interview.common.enums.*;
import interview.common.exception.BusinessException;
import interview.common.util.CryptoUtil;
import interview.common.util.JwttUtil;
import interview.common.util.TraceUtil;
import interview.framework.config.properties.JwtProperties;
import interview.framework.context.AuthContext;
import interview.common.constant.AuthKeyConstant;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.mapper.AuthMapper;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.entity.UserToken;
import interview.system.auth.model.enums.WorkspaceType;
import interview.system.auth.model.req.*;
import interview.system.auth.model.vo.RefreshTokenVO;
import interview.system.auth.model.vo.SecureChallengeStartVO;
import interview.system.auth.model.vo.TokenInfoVO;
import interview.system.auth.model.vo.UserEnterpriseVO;
import interview.system.auth.model.vo.WorkspaceSwitchVO;
import interview.system.rbac.mapper.PermissionsMapper;
import interview.system.rbac.model.entity.Role;
import interview.system.auth.model.entity.User;
import interview.system.rbac.model.entity.UserRole;
import interview.system.auth.model.enums.UserStatus;
import interview.system.rbac.service.*;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import interview.system.tenant.service.EnterprisesService;
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
    private final JwtProperties jwtProperties;
    private final SecureChallengeService secureChallengeService;

    @Override
    @Transactional
    public RegisterBo register(RegisterReq register) {

        // 验证code 并 删除code
        smsService.verifyCode(register.getPhone(), register.getCode(), SmsType.REGISTER);

        // 验证用户名、手机号、邮箱是否已存在
        checkUnique( register);

        // 构建用户
        User user = buildUser(register);
        usersService.save(user);
        Long number = user.getId();

        // FK检查
        Integer code = interview.common.enums.Role.CANDIDATE.getCode();
        if (!rolesService.lambdaQuery()
                .eq(Role::getId, code)
                .exists()) {
            log.error("外键拦截: 角色:[{}]在Role表内已不存在", interview.common.enums.Role.CANDIDATE);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        // 构建角色(默认求职者)
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(code);
        userRolesService.save(userRole);


        // 构建Token
        String raw = IdUtil.fastSimpleUUID();
        UserToken userToken = buildUserToken(raw, number);
        save(userToken);

        return buildRegisterBo(raw, user, number);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public LoginBO login(LoginReq login, String clientIp) {
        // 验证用户名、状态、密码
        User user = checkAndGetUser(login);
        return buildLoginResult(user, login.getDeviceInfo(), clientIp);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public LoginBO loginBySms(SmsLoginReq login, String clientIp) {
        // 手机号必须属于正常用户，随后原子核销登录验证码。
        User user = getLoginUserByPhone(login.getPhone());
        smsService.verifyCode(login.getPhone(), login.getCode(), SmsType.LOGIN);
        return buildLoginResult(user, login.getDeviceInfo(), clientIp);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void resetPassword(PasswordResetReq resetReq) {
        // 先确认手机号对应用户，再原子核销重置密码验证码。
        User user = usersService.lambdaQuery()
                .select(User::getId, User::getStatus)
                .eq(User::getPhone, resetReq.getPhone())
                .one();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != UserStatus.NORMAL) {
            throw new BusinessException(ErrorCode.USER_ALREADY_FREEZE);
        }
        smsService.verifyCode(resetReq.getPhone(), resetReq.getCode(), SmsType.RESET_PWD);

        // 密码修改成功后撤销该用户全部 Refresh Token，要求所有设备重新登录。
        boolean updated = usersService.lambdaUpdate()
                  .eq(User::getId, user.getId())
                  .set(User::getPasswordHash, CryptoUtil.hashPassword(resetReq.getNewPassword()))
                  .set(User::getUpdatedAt, OffsetDateTime.now())
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }
        lambdaUpdate()
                .eq(UserToken::getUserId, user.getId())
                  .eq(UserToken::getIsRevoked, false)
                  .set(UserToken::getIsRevoked, true)
                  .set(UserToken::getTraceId, null)
                  .update();
    }

    @Override
    @Transactional
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
            log.error("异常的登录状态：Refresh Token 不存在或已过期");
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        Long userId = token.getUserId();
        String ipAddress = token.getIpAddress();

        //FK检查
        User user = usersService.lambdaQuery()
                .select(
                        User::getStatus,
                        User::getUsername,
                        User::getUserType,
                        User::getRiskLevel
                )
                .eq(User::getId, userId)
                .one();
        if (user == null) {
            log.error("外键拦截: 外键UserId：[{}]在User表已不存在",userId);
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        if (user.getStatus() == UserStatus.DISABLED){
            throw new BusinessException(ErrorCode.USER_ALREADY_FREEZE);
        }

        // 触发重放检测
        if (Boolean.TRUE.equals(token.getIsRevoked())){
            log.error("检测到过期Refresh Token 再次被使用! userId:[{}], IP:[{}]", userId, ipAddress);

            boolean updated = lambdaUpdate()
                    .eq(UserToken::getUserId, userId)
                    .eq(UserToken::getDeviceInfo, token.getDeviceInfo())
                    .set(UserToken::getIsRevoked, true)
                    .set(UserToken::getTraceId, TraceUtil.getTraceId())
                    .update();

            if (!updated) {
                log.warn("重放撤销未影响任何记录: userId [{}]", userId);
            }

            throw new BusinessException(ErrorCode.RISK_CONTROL);
        }

        // 原子核销当前 Refresh Token：并发复用同一 token 时仅首个请求能核销成功，其余按重放处理。
        if (!consumeRefreshToken(refreshToken)) {
            log.error("检测到 Refresh Token 并发复用或状态异常, 触发族吊销! userId:[{}], IP:[{}], deviceInfo:[{}]",
                    userId, ipAddress, token.getDeviceInfo());
            revokeUserDeviceTokens(userId, token.getDeviceInfo());
            throw new BusinessException(ErrorCode.RISK_CONTROL);
        }

        // 生成refreshToken
        String raw = checkAndGetUserToken(userId, token.getDeviceInfo(), ipAddress);

        // 按客户端当前工作区恢复企业上下文，避免刷新后跳到最近加入的企业。
        Long enterpriseId = refresh.getEnterpriseId();
        if (enterpriseId != null) {
            loadEnterpriseContext(userId, enterpriseId);
        }
        RbacContext rbacContext = loadRbacContext(userId, enterpriseId);

        List<String> platformRoleCodes = rbacContext.platformRoleCodes;

        Map<String, Object> newClaims = new HashMap<>();
        newClaims.put("userId", userId);
        newClaims.put("username", user.getUsername());
        newClaims.put("userType", user.getUserType().name());
        newClaims.put("riskLevel", user.getRiskLevel().getCode());
        newClaims.put("platformRoleCodes", platformRoleCodes);
        newClaims.put("entRoleMap", rbacContext.entRoleMap);
        if (enterpriseId != null) {
            newClaims.put("enterpriseId", enterpriseId);
        }

        // 生成新的accessToken
        String accessToken = jwttUtil.generatorToken(newClaims, token.getUserId());

        return RefreshTokenVO.builder()
                    .accessToken(accessToken)
                    .refreshToken(raw)
                    .expiresInSeconds(jwtProperties.getExpiration() * 60)
                    .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public WorkspaceSwitchVO switchWorkspace(WorkspaceSwitchReq switchReq, String accessToken) {
        Long userId = AuthContext.getRequiredUserId();
        User user = usersService.lambdaQuery()
                .select(User::getUsername, User::getUserType, User::getRiskLevel, User::getStatus)
                .eq(User::getId, userId)
                .one();
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }
        if (user.getStatus() != UserStatus.NORMAL) {
            throw new BusinessException(ErrorCode.USER_ALREADY_FREEZE);
        }

        // 校验目标工作区；企业归属必须以当前数据库记录为准。
        Long enterpriseId = switchReq.getEnterpriseId();
        if (switchReq.getWorkspaceType() == WorkspaceType.ENTERPRISE) {
            if (enterpriseId == null) {
                throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
            }
            loadEnterpriseContext(userId, enterpriseId);
        } else {
            if (enterpriseId != null) {
                throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
            }
            if (user.getUserType() != UserType.PLATFORM_ADMIN
                    && user.getUserType() != UserType.PLATFORM_OPS) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED);
            }
        }
        RbacContext rbacContext = loadRbacContext(userId, enterpriseId);

        // 旧 JWT 加入黑名单（同 refreshToken 逻辑）
        try {
            Claims claims = jwttUtil.parseToken(accessToken);
            long expire = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (expire > 0) {
                stringRedisTemplate.opsForValue().set(
                        AuthKeyConstant.getTokenBanKey(accessToken),
                        "1",
                        expire,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception ignored) {
            // token 已过期或无效，无需加入黑名单
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType().name());
        claims.put("riskLevel", user.getRiskLevel().getCode());
        claims.put("platformRoleCodes", rbacContext.platformRoleCodes);
        claims.put("entRoleMap", rbacContext.entRoleMap);
        if (enterpriseId != null) {
            claims.put("enterpriseId", enterpriseId);
        }

        String newToken = jwttUtil.generatorToken(claims, userId);

        return new WorkspaceSwitchVO(
                switchReq.getWorkspaceType(),
                enterpriseId,
                newToken,
                jwtProperties.getExpiration() * 60L);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void logout(LogoutReq logout, String accessToken) {

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
        boolean updated = lambdaUpdate()
                    .eq(UserToken::getRefreshTokenHash, hashToken)
                    .eq(UserToken::getUserId,AuthContext.getRequiredUserId())
                    .set(UserToken::getIsRevoked, true)
                    .set(UserToken::getTraceId, TraceUtil.getTraceId())
                    .update();

        if (!updated) {
            log.warn("登出时未找到匹配的 Refresh Token: userId [{}]", AuthContext.getRequiredUserId());
        }
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
                .eq(UserToken::getUserId, AuthContext.getRequiredUserId())
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
    public SecureChallengeStartVO startRevokeChallenge(Long tokenId) {
        Long userId = AuthContext.getRequiredUserId();

        // 仅允许为当前用户仍然有效的设备 Token 创建下线 Challenge
        boolean exists = lambdaQuery()
                .eq(UserToken::getId, tokenId)
                .eq(UserToken::getUserId, userId)
                .eq(UserToken::getIsRevoked, false)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        SecureChallengeStartDTO challenge = secureChallengeService.create(
                userId, SecureActionType.REVOKE_DEVICE, tokenId);
        return new SecureChallengeStartVO(
                challenge.challengeId(),
                challenge.maskedPhone(),
                challenge.expiresInSeconds());
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void revoke(Long tokenId, SecureActionContext secureActionContext) {
        Long userId = AuthContext.getRequiredUserId();

        // 一次性令牌必须精确授权下线当前路径指定的设备 Token
        if (secureActionContext == null
                || secureActionContext.getActionType() != SecureActionType.REVOKE_DEVICE) {
            throw new BusinessException(ErrorCode.SECURE_ACTION_NOT_MATCH);
        }
        if (!tokenId.equals(secureActionContext.getResourceId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        // 更新设备 Token 状态并限制记录必须属于当前用户
        boolean update = lambdaUpdate()
                .eq(UserToken::getId, tokenId)
                .eq(UserToken::getUserId, userId)
                .set(UserToken::getIsRevoked, true)
                .set(UserToken::getTraceId, TraceUtil.getTraceId())
                .update();

        if (!update){
            log.warn("越权拦截或数据不存在: 用户 [{}] 试图作废 Token [{}]", userId, tokenId);
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        log.info("设备下线成功: 用户 [{}] 成功移除了设备 Token [{}]", userId, tokenId);
    }

    @Override
    public UserInfoBO getUserInfo() {

        Long userId = AuthContext.getRequiredUserId();
        User user = usersService.lambdaQuery()
                .select(
                    User::getUsername, User::getNickname,
                    User::getPhone, User::getEmail,
                    User::getAvatarUrl, User::getUserType,
                    User::getStatus
                )
                .eq(User::getId, userId)
                .one();

        if (user == null){
            log.error("{getUserInfo}——token有效，但数据库无此实体，userId：[{}]", userId);
            throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
        }

        if (user.getStatus() != UserStatus.NORMAL){
            log.error("用户已冻结，userId:[{}], userStatus:[{}]", userId, user.getStatus().name());
            throw new BusinessException(ErrorCode.USER_ALREADY_FREEZE);
        }

        // 复用登录流程的企业、角色与权限聚合规则，返回当前最新会话信息。
        EnterpriseContext enterpriseContext = loadEnterpriseContext(userId, AuthContext.getEnterpriseId());
        RbacContext rbacContext = loadRbacContext(userId, enterpriseContext.enterpriseId);

        return UserInfoBO.builder()
                .id(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .userType(user.getUserType().name())
                .enterpriseId(enterpriseContext.enterpriseId)
                .enterpriseName(enterpriseContext.name)
                .logoUrl(enterpriseContext.logoUrl)
                .enterprises(enterpriseContext.enterprises)
                .status(user.getStatus())
                .roles(rbacContext.platformRoleCodes)
                .permissions(rbacContext.permissions)
                .build();
    }


    private void checkUnique(RegisterReq register){
        //查库确认用户名是否存在
        boolean isExists = usersService.lambdaQuery()
                .eq(User::getUsername, register.getUsername())
                .exists();

        if (isExists) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        //效验手机号是否已存在
        isExists = usersService.lambdaQuery()
                .eq(User::getPhone, register.getPhone())
                .exists();
        if (isExists) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        //验邮箱是否存在
        if (StrUtil.isNotBlank(register.getEmail())) {
            isExists = usersService.lambdaQuery()
                    .eq(User::getEmail, register.getEmail())
                    .exists();
            if (isExists) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }
    }

    private User buildUser(RegisterReq register){
        return User.builder()
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

    private RegisterBo buildRegisterBo(String raw, User user, Long number){
        // 生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", number);
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType().name());
        claims.put("riskLevel", RiskLevel.NO_RISK.getCode());
        claims.put("platformRoleCodes", List.of());
        claims.put("entRoleMap", Map.of());
        String jwtToken = jwttUtil.generatorToken(claims, number);

        return RegisterBo.builder()
                .userId(number)
                .username(user.getUsername())
                .accessToken(jwtToken)
                .refreshToken(raw)
                .expiresInSeconds(jwtProperties.getExpiration() * 60L)
                .build();
    }

    private User checkAndGetUser(LoginReq login){
        User user = usersService.lambdaQuery()
                .select(
                        User::getId,
                        User::getUsername,
                        User::getPhone,
                        User::getEmail,
                        User::getPasswordHash,
                        User::getNickname,
                        User::getUserType,
                        User::getRiskLevel,
                        User::getStatus
                )
                .eq(User::getUsername, login.getUsername())
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

    private User getLoginUserByPhone(String phone) {
        User user = usersService.lambdaQuery()
                .select(
                        User::getId,
                        User::getUsername,
                        User::getPhone,
                        User::getEmail,
                        User::getNickname,
                        User::getAvatarUrl,
                        User::getUserType,
                        User::getRiskLevel,
                        User::getStatus
                )
                .eq(User::getPhone, phone)
                .one();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != UserStatus.NORMAL) {
            throw new BusinessException(ErrorCode.USER_ALREADY_FREEZE);
        }
        return user;
    }

    private LoginBO buildLoginResult(User user, String deviceInfo, String clientIp) {
        Long userId = user.getId();

        // 创建新设备令牌并加载可选企业及基础角色权限，不替用户选择企业。
        String raw = checkAndGetUserToken(userId, deviceInfo, clientIp);
        EnterpriseContext enterpriseContext = loadEnterpriseContext(userId);
        RbacContext rbacContext = loadRbacContext(userId, null);
        List<String> platformRoleCodes = rbacContext.platformRoleCodes;

        // 生成携带当前授权快照的短时 Access Token。
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType().name());
        claims.put("riskLevel", user.getRiskLevel().getCode());
        claims.put("platformRoleCodes", platformRoleCodes);
        claims.put("entRoleMap", rbacContext.entRoleMap);
        String token = jwttUtil.generatorToken(claims, userId);

        return LoginBO.builder()
                .userId(userId)
                .enterpriseId(null)
                .enterpriseName(enterpriseContext.name)
                .logoUrl(enterpriseContext.logoUrl)
                .enterprises(enterpriseContext.enterprises)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .userType(user.getUserType())
                .roles(platformRoleCodes)
                .permissions(rbacContext.permissions)
                .accessToken(token)
                .refreshToken(raw)
                .expiresInSeconds(jwtProperties.getExpiration() * 60)
                .build();
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
                    .set(UserToken::getTraceId, TraceUtil.getTraceId())
                    .update();

            save(userToken);
        return raw;
    }

    /**
     * 原子核销指定 Refresh Token。
     * 仅在目标记录未被撤销时返回 true，用于拦截同一 token 的并发复用（TOCTOU）。
     */
    private boolean consumeRefreshToken(String refreshToken) {
        return lambdaUpdate()
                .eq(UserToken::getRefreshTokenHash, DigestUtil.sha256Hex(refreshToken))
                .eq(UserToken::getIsRevoked, false)
                .set(UserToken::getIsRevoked, true)
                .set(UserToken::getTraceId, TraceUtil.getTraceId())
                .update();
    }

    /**
     * 吊销指定用户在指定设备上的全部 Refresh Token（token 族）。
     */
    private void revokeUserDeviceTokens(Long userId, String deviceInfo) {
        lambdaUpdate()
                .eq(UserToken::getUserId, userId)
                .eq(UserToken::getDeviceInfo, deviceInfo)
                .set(UserToken::getIsRevoked, true)
                .set(UserToken::getTraceId, TraceUtil.getTraceId())
                .update();
    }

    private EnterpriseContext loadEnterpriseContext(Long userId) {
        return loadEnterpriseContext(userId, null);
    }

    private EnterpriseContext loadEnterpriseContext(Long userId, Long preferredEnterpriseId) {
        // 查用户所有企业成员记录，按加入时间降序（最新加入的排第一）
        List<EnterpriseTeamMember> members = enterpriseTeamMembersService.lambdaQuery()
                .select(EnterpriseTeamMember::getEnterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .orderByDesc(EnterpriseTeamMember::getCreatedAt)
                .list();

        if (members.isEmpty()) {
            if (preferredEnterpriseId != null) {
                throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
            }
            return new EnterpriseContext(null, null, null, List.of());
        }

        List<Long> enterpriseIds = members.stream()
                .map(EnterpriseTeamMember::getEnterpriseId)
                .toList();

        // 批量查企业信息
        List<Enterprise> enterprises = enterprisesService.lambdaQuery()
                .select(Enterprise::getId, Enterprise::getName, Enterprise::getShortName,
                        Enterprise::getLogoUrl, Enterprise::getStatus)
                .in(Enterprise::getId, enterpriseIds)
                .list();

        List<UserEnterpriseVO> enterpriseBriefs = enterprises.stream()
                .map(e -> UserEnterpriseVO.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .shortName(e.getShortName())
                        .logoUrl(e.getLogoUrl())
                        .status(e.getStatus())
                        .build())
                .toList();

        // 未显式选择企业时仅返回列表，不生成活跃企业上下文。
        if (preferredEnterpriseId == null) {
            return new EnterpriseContext(null, null, null, enterpriseBriefs);
        }
        if (!enterpriseIds.contains(preferredEnterpriseId)) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }
        Long activeEnterpriseId = preferredEnterpriseId;
        Enterprise activeEnterprise = enterprises.stream()
                .filter(e -> e.getId().equals(activeEnterpriseId))
                .findFirst()
                .orElse(null);

        if (activeEnterprise == null) {
            log.error("脏数据拦截: 用户ID [{}], 其关联的企业ID [{}] 在 enterprises 表中查不到实体记录！", userId, activeEnterpriseId);
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }

        return new EnterpriseContext(activeEnterpriseId, activeEnterprise.getName(), activeEnterprise.getLogoUrl(), enterpriseBriefs);
    }

    private RbacContext loadRbacContext(Long userId,Long enterpriseId) {
        // 平台角色 — sys_user_roles
        List<Integer> platformRoleIds = userRolesService.lambdaQuery()
                .select(UserRole::getRoleId)
                .eq(UserRole::getUserId, userId)
                .list()
                .stream()
                .map(UserRole::getRoleId)
                .distinct()
                .toList();

        List<String> platformRoleCodes = platformRoleIds.isEmpty()
                ? List.of()
                : rolesService.lambdaQuery()
                    .select(Role::getRoleCode)
                    .in(Role::getId, platformRoleIds)
                    .list()
                    .stream()
                    .map(Role::getRoleCode)
                    .toList();

        // 企业角色 — enterprise_team_members（全企业）
        List<EnterpriseTeamMember> teamMembers = enterpriseTeamMembersService.lambdaQuery()
                .select(EnterpriseTeamMember::getEnterpriseId, EnterpriseTeamMember::getRoleId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .list();

        Map<Long, List<String>> entRoleMap = new HashMap<>();
        if (!teamMembers.isEmpty()) {
            Map<Long, List<Integer>> entRoleIdsMap = new HashMap<>();
            for (EnterpriseTeamMember member : teamMembers) {
                Long entId = member.getEnterpriseId();
                entRoleIdsMap.computeIfAbsent(entId, k -> new ArrayList<>()).add(member.getRoleId());
            }

            List<Integer> allEnterpriseRoleIds = entRoleIdsMap.values().stream()
                    .flatMap(List::stream)
                    .toList();

            Map<Integer, String> roleCodeMap = rolesService.lambdaQuery()
                    .select(Role::getId, Role::getRoleCode)
                    .in(Role::getId, allEnterpriseRoleIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Role::getId, Role::getRoleCode));

            for (Map.Entry<Long, List<Integer>> entry : entRoleIdsMap.entrySet()) {
                Long entId = entry.getKey();
                List<String> codes = entry.getValue().stream()
                        .map(roleCodeMap::get)
                        .toList();
                entRoleMap.put(entId, codes);
            }
        }

        // 只汇总基础角色与当前企业角色，避免其他企业权限进入当前工作区。
        List<Integer> allRoleIds = new ArrayList<>(platformRoleIds);
        if (enterpriseId != null) {
            teamMembers.stream()
                    .filter(member -> enterpriseId.equals(member.getEnterpriseId()))
                    .map(EnterpriseTeamMember::getRoleId)
                    .forEach(allRoleIds::add);
        }

        if (CollUtil.isEmpty(allRoleIds)) {
            if (teamMembers.isEmpty()) {
                log.error("脏数据拦截: 用户ID [{}] 未关联任何有效角色", userId);
                throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
            }
            return new RbacContext(platformRoleCodes, entRoleMap, List.of());
        }

        List<String> permissions = permissionsMapper.getPermCodeByRoleId(
                allRoleIds.stream().distinct().toList());

        return new RbacContext(platformRoleCodes, entRoleMap, permissions);
    }

    // 企业上下文载体
    private record EnterpriseContext(
            Long enterpriseId,
            String name,
            String logoUrl,
            List<UserEnterpriseVO> enterprises) {}

    // 权限上下文载体
    private record RbacContext(
            List<String> platformRoleCodes,
            Map<Long,List<String>> entRoleMap,
            List<String> permissions) {}
}
