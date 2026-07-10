package interview.system.auth;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.enums.SmsType;
import interview.system.auth.model.req.SmsSendReq;
import interview.system.auth.model.entity.SysUser;
import interview.system.auth.service.impl.SmsServiceImpl;
import interview.system.auth.service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static interview.system.TestMockUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private UsersService usersService;

    @InjectMocks
    private SmsServiceImpl smsService;

    private final String phone = "13812345678";
    private final SmsSendReq req = new SmsSendReq();

    @BeforeEach
    void setUp() {
        req.setPhone(phone);
        req.setSmsType(SmsType.REGISTER);
    }

    @Nested
    class SendSms {

        @Test
        void sendSms_success() {
            LambdaQueryChainWrapper<SysUser> q = mockQueryWrapper();

            when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(usersService.lambdaQuery()).thenReturn(q);

            smsService.sendSms(req);

            verify(stringRedisTemplate, atLeastOnce()).opsForValue();
        }

        @Test
        void sendSms_fail_rateLimited() {
            when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class, () -> smsService.sendSms(req));
            assertEquals(ErrorCode.CODE_ONE_MINUTE.getCode(), ex.getCode());
        }

        @Test
        void sendSms_fail_phoneAlreadyRegistered() {
            LambdaQueryChainWrapper<SysUser> q = mockQueryWrapper();
            when(q.one()).thenReturn(SysUser.builder().build());

            when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
            when(usersService.lambdaQuery()).thenReturn(q);

            BusinessException ex = assertThrows(BusinessException.class, () -> smsService.sendSms(req));
            assertEquals(ErrorCode.PHONE_ALREADY_EXISTS.getCode(), ex.getCode());
        }
    }

    @Nested
    class VerifyCode {

        @Test
        void verifyCode_success() {
            when(stringRedisTemplate.execute(any(), anyList(), anyString())).thenReturn(2L);

            assertDoesNotThrow(() -> smsService.verifyCode(phone, "1234", SmsType.REGISTER));
        }

        @Test
        void verifyCode_fail_expired() {
            when(stringRedisTemplate.execute(any(), anyList(), anyString())).thenReturn(0L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> smsService.verifyCode(phone, "1234", SmsType.REGISTER));
            assertEquals(ErrorCode.CODE_ERROR_OR_EXPIRED.getCode(), ex.getCode());
        }

        @Test
        void verifyCode_fail_wrongCode() {
            when(stringRedisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> smsService.verifyCode(phone, "1234", SmsType.REGISTER));
            assertEquals(ErrorCode.CODE_ERROR.getCode(), ex.getCode());
        }
    }
}
