package interview.common.annonate;

import interview.common.enums.SecureActionType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSecure {

    /**
     * 接口要求的一次性安全授权动作。
     * @return 安全操作类型
     */
    SecureActionType value();
}
