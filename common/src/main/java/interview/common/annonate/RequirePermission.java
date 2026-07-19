package interview.common.annonate;

import interview.common.enums.Logic;
import interview.common.enums.PermissionScope;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    String[] permissions();

    PermissionScope scope() default PermissionScope.PLATFORM;

    Logic logic() default Logic.AND;
}
