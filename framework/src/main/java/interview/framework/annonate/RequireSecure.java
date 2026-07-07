package interview.framework.annonate;


import interview.common.enums.SmsType;

import java.lang.annotation.*;

/**
 * @author zhuxi
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSecure {
    SmsType[] allowList();
}
