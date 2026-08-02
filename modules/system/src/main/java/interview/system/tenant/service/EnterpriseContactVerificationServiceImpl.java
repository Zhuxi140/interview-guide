package interview.system.tenant.service;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import interview.common.constant.AuthKeyConstant;
import interview.common.constant.SecureActionContext;
import interview.common.enums.ErrorCode;
import interview.common.enums.SecureActionType;
import interview.common.enums.SmsType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.system.auth.model.entity.User;
import interview.system.auth.service.SmsService;
import interview.system.auth.service.UsersService;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.enums.EnterpriseContactVerifyStage;
import interview.system.tenant.model.enums.EnterpriseStatus;
import interview.system.tenant.model.req.EnterpriseContactCodeVerifyReq;
import interview.system.tenant.model.req.EnterpriseContactNewPhoneReq;
import interview.system.tenant.model.vo.EnterpriseContactVerifyStartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 企业联系电话双重验证流程实现，负责维护 Redis 验证状态并签发安全操作令牌。
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class EnterpriseContactVerificationServiceImpl
        implements EnterpriseContactVerificationService {

    private static final long FLOW_TTL_MINUTES = 15;
    private static final long SECURE_TOKEN_TTL_SECONDS = 300;
    private static final DefaultRedisScript<String> VERIFY_CODE_SCRIPT;
    private static final DefaultRedisScript<String> BIND_NEW_PHONE_SCRIPT;

    // 加载企业联系电话验证使用的 Redis Lua 原子脚本。
    static {
        VERIFY_CODE_SCRIPT = new DefaultRedisScript<>();
        VERIFY_CODE_SCRIPT.setScriptText(
                ResourceUtil.readUtf8Str("EnterpriseContactVerifyCode.lua"));
        VERIFY_CODE_SCRIPT.setResultType(String.class);

        BIND_NEW_PHONE_SCRIPT = new DefaultRedisScript<>();
        BIND_NEW_PHONE_SCRIPT.setScriptText(
                ResourceUtil.readUtf8Str("EnterpriseContactBindNewPhone.lua"));
        BIND_NEW_PHONE_SCRIPT.setResultType(String.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final SmsService smsService;
    private final UsersService usersService;
    private final EnterprisesService enterprisesService;
    private final EnterpriseTeamMembersService enterpriseTeamMembersService;

    @Override
    public EnterpriseContactVerifyStartVO start(Long enterpriseId) {
        Long userId = AuthContext.getRequiredUserId();

        // 查询企业并确认当前用户具有企业成员身份。
        Enterprise enterprise = getEnterpriseAndCheckMember(enterpriseId, userId);

        // 验证用户存在
        User user = usersService.lambdaQuery()
                .select(User::getId, User::getPhone)
                .eq(User::getId, userId)
                .one();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 比较企业原联系电话与当前用户手机号，决定是否需要验证原号码
        String oldPhone = enterprise.getContactPhone();
        boolean oldVerificationRequired = StrUtil.isNotBlank(oldPhone)
                && !Objects.equals(oldPhone, user.getPhone());

        // 需要验证原号码时等待验证码，否则直接进入原号码已验证状态
        EnterpriseContactVerifyStage initialStage = oldVerificationRequired
                ? EnterpriseContactVerifyStage.WAIT_OLD_VERIFY
                : EnterpriseContactVerifyStage.OLD_VERIFIED;

        // 生成流程 ID，并将流程归属、号码快照及当前状态写入 Redis Hash
        String flowId = IdUtil.fastSimpleUUID();
        String flowKey = AuthKeyConstant.getEnterpriseContactFlowKey(flowId);

        Map<String, String> flow = new HashMap<>();
        flow.put(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_USER_ID, userId.toString());
        flow.put(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_ENTERPRISE_ID, enterpriseId.toString());
        flow.put(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_OLD_PHONE, StrUtil.nullToEmpty(oldPhone));
        flow.put(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_NEW_PHONE, "");
        flow.put(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_STAGE, initialStage.name());
        stringRedisTemplate.opsForHash().putAll(flowKey, flow);
        stringRedisTemplate.expire(flowKey, FLOW_TTL_MINUTES, TimeUnit.MINUTES);

        // 企业原联系电话与当前用户手机号不一致时，向企业原联系电话发送验证码
        if (oldVerificationRequired) {
            try {
                smsService.sendCode(
                        oldPhone,
                        SmsType.ENTERPRISE_VERIFY_OLD,
                        AuthKeyConstant.getEnterpriseContactCodeKey(
                                flowId, SmsType.ENTERPRISE_VERIFY_OLD));
            } catch (Exception e) {
                // 验证码发送失败时删除已创建的流程，避免残留不可继续的状态
                stringRedisTemplate.delete(flowKey);
                throw e;
            }
        }

        return new EnterpriseContactVerifyStartVO(flowId, oldVerificationRequired);
    }

    @Override
    public void verifyOldPhone(Long enterpriseId, EnterpriseContactCodeVerifyReq req) {
        Long userId = AuthContext.getRequiredUserId();

        // 流程执行期间仍需确认企业允许维护认证资料。
        getEnterpriseAndCheckMember(enterpriseId, userId);

        // 原子校验流程归属、当前状态和验证码，并推进为原号码已验证
        verifyCodeTransition(
                req.getFlowId(),
                enterpriseId,
                userId,
                SmsType.ENTERPRISE_VERIFY_OLD,
                EnterpriseContactVerifyStage.WAIT_OLD_VERIFY,
                EnterpriseContactVerifyStage.OLD_VERIFIED,
                req.getCode(),
                "",
                "");
    }

    @Override
    public void sendNewPhoneCode(Long enterpriseId, EnterpriseContactNewPhoneReq req) {
        Long userId = AuthContext.getRequiredUserId();

        // 发送短信前重新确认企业未被暂停或注销。
        getEnterpriseAndCheckMember(enterpriseId, userId);

        // 读取流程并校验流程属于当前用户和企业
        Map<String, String> flow = getFlow(req.getFlowId());
        validateFlowOwner(flow, enterpriseId, userId);

        // 新联系电话不得与流程创建时记录的企业原联系电话相同
        if (Objects.equals(
                flow.get(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_OLD_PHONE),
                req.getNewPhone())) {
            throw new BusinessException(ErrorCode.PHONE_SAME_AS_OLD);
        }

        // 原子绑定新联系电话，并将状态从原号码已验证推进为等待新号码验证
        String result = stringRedisTemplate.execute(
                BIND_NEW_PHONE_SCRIPT,
                java.util.Collections.singletonList(
                        AuthKeyConstant.getEnterpriseContactFlowKey(req.getFlowId())),
                userId.toString(),
                enterpriseId.toString(),
                req.getNewPhone(),
                EnterpriseContactVerifyStage.OLD_VERIFIED.name(),
                EnterpriseContactVerifyStage.WAIT_NEW_VERIFY.name());

        handleVerifyResult(result);

        // 状态推进成功后向新联系电话发送第二重验证码
        smsService.sendCode(
                req.getNewPhone(),
                SmsType.ENTERPRISE_VERIFY_NEW,
                AuthKeyConstant.getEnterpriseContactCodeKey(
                        req.getFlowId(), SmsType.ENTERPRISE_VERIFY_NEW));
    }

    @Override
    public String verifyNewPhone(Long enterpriseId, EnterpriseContactCodeVerifyReq req) {
        Long userId = AuthContext.getRequiredUserId();

        // 签发安全令牌前重新确认企业仍可维护认证资料。
        getEnterpriseAndCheckMember(enterpriseId, userId);

        // 读取并校验流程归属，阻止跨用户或跨企业使用 flowId
        Map<String, String> flow = getFlow(req.getFlowId());
        validateFlowOwner(flow, enterpriseId, userId);

        // 重复提交已完成的验证时，仅返回仍然有效的原安全令牌
        if (EnterpriseContactVerifyStage.NEW_VERIFIED.name().equals(
                flow.get(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_STAGE))) {
            String existingToken = flow.get(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_SECURE_TOKEN);
            if (StrUtil.isBlank(existingToken)
                    || !Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                    AuthKeyConstant.getSecureActionTokenKey(existingToken)))) {
                throw new BusinessException(ErrorCode.CONTACT_VERIFY_FLOW_INVALID);
            }
            return existingToken;
        }

        // 构建绑定用户、企业、新旧号码快照的安全操作上下文
        String secureToken = req.getFlowId() + "." + IdUtil.fastSimpleUUID();
        SecureActionContext context = SecureActionContext.builder()
                .userId(userId)
                .actionType(SecureActionType.UPDATE_ENTERPRISE_PHONE)
                .resourceId(enterpriseId)
                .targetPhone(flow.get(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_NEW_PHONE))
                .enterpriseId(enterpriseId)
                .sourcePhone(flow.get(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_OLD_PHONE))
                .challengeId(req.getFlowId())
                .build();

        // 原子核销新号码验证码、推进流程状态并写入一次性安全令牌
        String result = verifyCodeTransition(
                req.getFlowId(),
                enterpriseId,
                userId,
                SmsType.ENTERPRISE_VERIFY_NEW,
                EnterpriseContactVerifyStage.WAIT_NEW_VERIFY,
                EnterpriseContactVerifyStage.NEW_VERIFIED,
                req.getCode(),
                secureToken,
                JSONUtil.toJsonStr(context));

        // 从 Lua 返回结果中取得最终签发的令牌
        String issuedToken = result.substring(result.indexOf(':') + 1);
        if (StrUtil.isBlank(issuedToken)) {
            throw new BusinessException(ErrorCode.CONTACT_VERIFY_FLOW_INVALID);
        }
        return issuedToken;
    }

    @Override
    public void complete(String flowId) {
        // 通用敏感操作令牌不绑定企业联系电话流程，无需清理
        if (StrUtil.isBlank(flowId)) {
            return;
        }

        // 联系方式更新成功后删除流程及可能残留的新旧号码验证码
        stringRedisTemplate.delete(java.util.List.of(
                AuthKeyConstant.getEnterpriseContactFlowKey(flowId),
                AuthKeyConstant.getEnterpriseContactCodeKey(
                        flowId, SmsType.ENTERPRISE_VERIFY_OLD),
                AuthKeyConstant.getEnterpriseContactCodeKey(
                        flowId, SmsType.ENTERPRISE_VERIFY_NEW)));
    }

    private String verifyCodeTransition(
            String flowId,
            Long enterpriseId,
            Long userId,
            SmsType smsType,
            EnterpriseContactVerifyStage expectedStage,
            EnterpriseContactVerifyStage nextStage,
            String code,
            String secureToken,
            String secureContextJson) {
        // 计算流程、验证码和安全令牌三个 Redis key
        String flowKey = AuthKeyConstant.getEnterpriseContactFlowKey(flowId);
        String codeKey = AuthKeyConstant.getEnterpriseContactCodeKey(flowId, smsType);
        String secureTokenKey = StrUtil.isBlank(secureToken)
                ? flowKey
                : AuthKeyConstant.getSecureActionTokenKey(secureToken);

        // 通过 Lua 原子完成归属校验、验证码核销、状态流转和令牌写入
        String result = stringRedisTemplate.execute(
                VERIFY_CODE_SCRIPT,
                java.util.List.of(flowKey, codeKey, secureTokenKey),
                userId.toString(),
                enterpriseId.toString(),
                expectedStage.name(),
                nextStage.name(),
                code,
                secureToken,
                secureContextJson,
                Long.toString(SECURE_TOKEN_TTL_SECONDS));

        // 将 Lua 协议返回值统一转换为业务结果或业务异常
        handleVerifyResult(result);
        return result;
    }

    private Enterprise getEnterpriseAndCheckMember(Long enterpriseId, Long userId) {
        // 查询企业及联系电话，不加载本流程不需要的其他字段
        Enterprise enterprise = enterprisesService.lambdaQuery()
                .select(Enterprise::getId, Enterprise::getContactPhone, Enterprise::getStatus)
                .eq(Enterprise::getId, enterpriseId)
                .one();
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
        if (enterprise.getStatus() == EnterpriseStatus.PAUSED) {
            throw new BusinessException(ErrorCode.ENTERPRISE_FROZEN);
        }
        if (enterprise.getStatus() != EnterpriseStatus.PENDING
                && enterprise.getStatus() != EnterpriseStatus.NORMAL) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        // 校验当前用户属于目标企业
        boolean member = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .exists();
        if (!member) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }
        return enterprise;
    }

    private Map<String, String> getFlow(String flowId) {
        // 读取流程 Hash，空数据表示流程不存在或已过期
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(
                AuthKeyConstant.getEnterpriseContactFlowKey(flowId));
        if (entries.isEmpty()) {
            throw new BusinessException(ErrorCode.CONTACT_VERIFY_FLOW_INVALID);
        }

        // 统一转换为字符串 Map，供后续状态和归属校验使用
        Map<String, String> flow = new HashMap<>();
        entries.forEach((key, value) -> flow.put(key.toString(), value.toString()));
        return flow;
    }

    private void validateFlowOwner(
            Map<String, String> flow, Long enterpriseId, Long userId) {
        // flowId 必须同时绑定当前登录用户和路径中的企业
        if (!userId.toString().equals(
                flow.get(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_USER_ID))
                || !enterpriseId.toString().equals(
                flow.get(AuthKeyConstant.ENTERPRISE_CONTACT_FLOW_FIELD_ENTERPRISE_ID))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
    }

    private void handleBindNewPhoneResult(Long result) {
        // 按 Lua 返回码映射流程不存在、归属错误和状态错误
        if (result == null || result == 0L) {
            throw new BusinessException(ErrorCode.CONTACT_VERIFY_FLOW_INVALID);
        }
        if (result == 1L) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        if (result == 2L) {
            throw new BusinessException(ErrorCode.CONTACT_VERIFY_STATE_INVALID);
        }
    }

    private void handleVerifyResult(String result) {
        // 按 Lua 返回码映射流程、权限、状态和验证码异常
        if (StrUtil.isBlank(result) || result.startsWith("0:")) {
            throw new BusinessException(ErrorCode.CONTACT_VERIFY_FLOW_INVALID);
        }
        if (result.startsWith("1:")) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        if (result.startsWith("2:")) {
            throw new BusinessException(ErrorCode.CONTACT_VERIFY_STATE_INVALID);
        }
        if (result.startsWith("3:")) {
            throw new BusinessException(ErrorCode.CODE_EXPIRED);
        }
        if (result.startsWith("4:")) {
            throw new BusinessException(ErrorCode.CODE_ERROR);
        }
        if (!result.startsWith("5:") && !result.startsWith("6:")) {
            throw new BusinessException(ErrorCode.CONTACT_VERIFY_FLOW_INVALID);
        }
    }
}
