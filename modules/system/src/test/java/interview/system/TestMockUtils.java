package interview.system;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public final class TestMockUtils {

    private static final Answer<Object> SELF_ANSWER = invocation -> {
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

    @SuppressWarnings("unchecked")
    public static <T> LambdaQueryChainWrapper<T> mockQueryWrapper() {
        return mock(LambdaQueryChainWrapper.class, withSettings().defaultAnswer(SELF_ANSWER));
    }

    @SuppressWarnings("unchecked")
    public static <T> LambdaUpdateChainWrapper<T> mockUpdateWrapper() {
        return mock(LambdaUpdateChainWrapper.class, withSettings().defaultAnswer(SELF_ANSWER));
    }

    private TestMockUtils() {}
}
