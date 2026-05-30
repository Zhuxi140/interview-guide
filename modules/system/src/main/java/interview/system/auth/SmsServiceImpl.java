package interview.system.auth;

import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.auth.enums.SmsType;
import interview.system.auth.model.req.SmsSendReq;
import interview.system.rbac.model.entity.SysUser;
import interview.system.rbac.service.UsersService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

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
public class SmsServiceImpl implements SmsService{

    private final StringRedisTemplate stringRedisTemplate;
    private final UsersService usersService;

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

        if (!exists) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        Random random = new Random();
        // 使用Random生成4位随机数
        String code = String.valueOf(random.nextInt(10000));
        log.error("发送短信验证码: code={},phone={}", code, phone);

        stringRedisTemplate.opsForValue().set(AuthKeyConstant.getSmsCodeKey(phone, smsType), code, 5, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(AuthKeyConstant.getSmsLockKey(phone, smsType), "1", 60, TimeUnit.SECONDS);
    }
}
