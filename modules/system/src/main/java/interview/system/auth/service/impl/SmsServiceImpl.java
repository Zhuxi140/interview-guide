package interview.system.auth.service.impl;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.common.constant.AuthKeyConstant;
import interview.common.constant.SecureActionContext;
import interview.common.enums.SmsType;
import interview.system.auth.model.req.SmsSendReq;
import interview.system.auth.model.entity.SysUser;
import interview.system.auth.model.req.VerifyReq;
import interview.system.auth.service.SmsService;
import interview.system.auth.service.UsersService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author zhuxi
 * @apiNote 短信服务实现
 */

@Slf4j
@Service
@AllArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UsersService usersService;
    private static final DefaultRedisScript<Long> CHECK_DELETE_CODE_SCRIPT;

    static{
        CHECK_DELETE_CODE_SCRIPT = new DefaultRedisScript<>();
        CHECK_DELETE_CODE_SCRIPT.setScriptText(ResourceUtil.readUtf8Str("GetDeleteCode.lua"));
        CHECK_DELETE_CODE_SCRIPT.setResultType(Long.class);
    }

    // 发送短信验证码，按 smsType 分流校验
    @Override
    public void sendSms(SmsSendReq smsReq) {

        SmsType smsType = smsReq.getSmsType();
        String phone = smsReq.getPhone();

        // 检查是否已发送短信
        if (stringRedisTemplate.hasKey(AuthKeyConstant.getSmsLockKey(phone, smsType))) {
            throw new BusinessException(ErrorCode.CODE_ONE_MINUTE);
        }

        SysUser user = usersService.lambdaQuery()
                .select(SysUser::getId,SysUser::getPhone)
                .eq(SysUser::getPhone, phone)
                .one();

        // 注册流程：校验手机号是否已被注册
        if (SmsType.REGISTER == smsType) {
            if (user != null){
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        // 非注册流程：校验手机号归属
        }else{
            if (user != null) {
                Long userId = AuthContext.getRequiredUserId();
                if (SmsType.DOUBLE_VERIFY_OLD != smsType) {
                    if (!user.getId().equals(userId)) {
                        throw new BusinessException(ErrorCode.NOT_YOUR_PHONE);
                    }
                }
                // 如果是双重手机验证，则允许当前登录用户手机号和要发送验证码的手机号可以不一致
            }else{
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
        }

        Random random = new Random();
        // 使用Random生成4位随机数
        String code = String.valueOf(random.nextInt(10000));
        log.error("发送短信验证码: code={},phone={}", code, phone);

        stringRedisTemplate.opsForValue().set(AuthKeyConstant.getSmsCodeKey(phone, smsType), code, 5, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(AuthKeyConstant.getSmsLockKey(phone, smsType), "1", 60, TimeUnit.SECONDS);
    }

    // Lua 原子校验 + 删除验证码
    @Override
    public void verifyCode(String phone, String code, SmsType type) {
        Long result = stringRedisTemplate.execute(
                CHECK_DELETE_CODE_SCRIPT,
                Collections.singletonList(AuthKeyConstant.getSmsCodeKey(phone, type)),
                code);

        if(result == null || result == 0L){
            throw new BusinessException(ErrorCode.CODE_ERROR_OR_EXPIRED);
        }else if(result == 1L){
            throw new BusinessException(ErrorCode.CODE_ERROR);
        }
    }

    // 核销验证码并颁发安全操作票据
    @Override
    public String verifyForSensitiveAction(VerifyReq verifyReq) {
        String phone = verifyReq.getPhone();
        SmsType smsType = verifyReq.getSmsType();

        // 双重验证：校验前序票据是否有效
        if (SmsType.DOUBLE_VERIFY_NEW == smsType){
            String previousToken = verifyReq.getPreviousToken();
            if (StrUtil.isBlank(previousToken)){
                throw new BusinessException(ErrorCode.VERIFY_NO_PRE_TOKEN);
            }

            String preKey = AuthKeyConstant.getSmsSensitiveActionTokenKey(previousToken);
            String preJson = stringRedisTemplate.opsForValue().get(preKey);
            if (StrUtil.isBlank(preJson)){
                throw new BusinessException(ErrorCode.VERIFY_PRE_TOKEN_VALID_OR_EXPIRED);
            }
            SecureActionContext bean = JSONUtil.toBean(preJson, SecureActionContext.class);

            if (!bean.getUserId().equals(AuthContext.getRequiredUserId())){
                throw new BusinessException(ErrorCode.PERMISSION_DENIED);
            }

            stringRedisTemplate.delete(preKey);
        }
        verifyCode(phone,verifyReq.getCode(),smsType);
        // 生成安全操作票据，写入 Redis（5 分钟有效）
        String token = IdUtil.fastSimpleUUID();
        String tokenKey = AuthKeyConstant.getSmsSensitiveActionTokenKey(token);
        SecureActionContext build = SecureActionContext.builder()
                .userId(AuthContext.getRequiredUserId())
                .actionType(smsType)
                .targetPhone(phone)
                .build();
        String json = JSONUtil.toJsonStr(build);
        stringRedisTemplate.opsForValue().set(tokenKey, json, 5, TimeUnit.MINUTES);
        return token;
    }
}
