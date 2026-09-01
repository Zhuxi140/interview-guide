package interview.system.auth.service;

import cn.hutool.core.io.resource.ResourceUtil;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.constant.AuthKeyConstant;
import interview.common.enums.SmsType;
import interview.system.auth.model.req.SmsSendReq;
import interview.system.auth.model.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author zhuxi
 * @apiNote 短信服务实现
 */

@Service
@AllArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UsersService usersService;
    private static final DefaultRedisScript<Long> CHECK_DELETE_CODE_SCRIPT;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static{
        CHECK_DELETE_CODE_SCRIPT = new DefaultRedisScript<>();
        CHECK_DELETE_CODE_SCRIPT.setScriptText(ResourceUtil.readUtf8Str("GetDeleteCode.lua"));
        CHECK_DELETE_CODE_SCRIPT.setResultType(Long.class);
    }

    @Override
    public void sendSms(SmsSendReq smsReq) {
        SmsType smsType = smsReq.getSmsType();
        String phone = smsReq.getPhone();

        // 查询手机号对应用户，供公开短信类型执行注册状态校验
        User user = usersService.lambdaQuery()
                .select(User::getId, User::getPhone)
                .eq(User::getPhone, phone)
                .one();

        // 注册要求手机号未占用，登录和重置密码要求手机号已注册
        switch (smsType) {
            case REGISTER -> {
                if (user != null) {
                    throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
                }
            }
            case LOGIN, RESET_PWD -> {
                if (user == null) {
                    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
                }
            }
            default -> throw new BusinessException(ErrorCode.SMS_TYPE_NOT_ALLOWED);
        }

        // 业务规则校验通过后生成并保存验证码
        sendCode(phone, smsType, AuthKeyConstant.getSmsCodeKey(phone, smsType));
    }

    @Override
    public void sendCode(String phone, SmsType type, String codeKey) {
        // 发送冷却期内拒绝重复生成验证码
        if (stringRedisTemplate.hasKey(AuthKeyConstant.getSmsLockKey(phone, type))) {
            throw new BusinessException(ErrorCode.CODE_ONE_MINUTE);
        }

        // 生成六位验证码；后续接入短信供应商，生产环境禁止记录验证码原文
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        // 验证码保存五分钟，同时写入一分钟发送冷却标记
        stringRedisTemplate.opsForValue().set(codeKey, code, 5, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(
                AuthKeyConstant.getSmsLockKey(phone, type), "1", 60, TimeUnit.SECONDS);
    }

    @Override
    public void verifyCode(String phone, String code, SmsType type) {
        // 通过 Lua 原子比较并删除验证码，防止同一验证码被重复使用
        Long result = stringRedisTemplate.execute(
                CHECK_DELETE_CODE_SCRIPT,
                Collections.singletonList(AuthKeyConstant.getSmsCodeKey(phone, type)),
                code);

        // 将 Lua 返回值转换为验证码过期或验证码错误
        if(result == null || result == 0L){
            throw new BusinessException(ErrorCode.CODE_ERROR_OR_EXPIRED);
        }else if(result == 1L){
            throw new BusinessException(ErrorCode.CODE_ERROR);
        }
    }

}
