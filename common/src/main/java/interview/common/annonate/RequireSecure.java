package interview.common.annonate;

import interview.common.enums.SmsType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSecure {
    SmsType[] allowList();
}
