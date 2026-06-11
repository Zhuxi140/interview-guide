package interview.system.auth.service.impl;

import cn.hutool.core.io.resource.ResourceUtil;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.auth.AuthKeyConstant;
import interview.system.auth.model.enums.SmsType;
import interview.system.auth.model.req.SmsSendReq;
import interview.system.auth.model.entity.SysUser;
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
 * @since 2026/5/27 14:05
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

    @Override
    public void sendSms(SmsSendReq smsReq) {

        SmsType smsType = smsReq.getSmsType();
        String phone = smsReq.getPhone();

        // 检查是否已发送短信
        if (stringRedisTemplate.hasKey(AuthKeyConstant.getSmsLockKey(phone, smsType))) {
            throw new BusinessException(ErrorCode.CODE_ONE_MINUTE);
        }

        boolean exists = usersService.lambdaQuery()
                .eq(SysUser::getPhone, phone)
                .exists();

        if (exists) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        Random random = new Random();
        // 使用Random生成4位随机数
        String code = String.valueOf(random.nextInt(10000));
        log.error("发送短信验证码: code={},phone={}", code, phone);

        stringRedisTemplate.opsForValue().set(AuthKeyConstant.getSmsCodeKey(phone, smsType), code, 5, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(AuthKeyConstant.getSmsLockKey(phone, smsType), "1", 60, TimeUnit.SECONDS);
    }

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
}
