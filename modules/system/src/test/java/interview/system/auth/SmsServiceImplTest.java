package interview.system.auth;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.auth.model.enums.SmsType;
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
import org.mockito.stubbing.Answer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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

    private final Answer<Object> SELF_ANSWER = invocation -> {
        Class<?> rt = invocation.getMethod().getReturnType();
        String name = invocation.getMethod().getName();
        if (Wrapper.class.isAssignableFrom(rt)) {
            return invocation.getMock();
        }
        if (rt == Object.class && !name.equals("one") && !name.equals("getEntity")) {
            return invocation.getMock();
        }
        return Mockito.RETURNS_DEFAULTS.answer(invocation);
    };

    @BeforeEach
    void setUp() {
        req.setPhone(phone);
        req.setSmsType(SmsType.REGISTER);
    }

    @Nested
    class SendSms {

        @SuppressWarnings("unchecked")
        private LambdaQueryChainWrapper<SysUser> mockQueryWrapper() {
            return Mockito.mock(LambdaQueryChainWrapper.class,
                    Mockito.withSettings().defaultAnswer(SELF_ANSWER));
        }

        @Test
        void sendSms_success() {
            LambdaQueryChainWrapper<SysUser> q = mockQueryWrapper();
            when(q.exists()).thenReturn(false);

            when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(usersService.lambdaQuery()).thenReturn(q);

            smsService.sendSms(req);

            verify(stringRedisTemplate, times(2)).opsForValue();
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
            when(q.exists()).thenReturn(true);

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
