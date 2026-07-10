package interview.framework.annonate;

import interview.framework.security.validation.ValidRoleValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * @author zhuxi
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRoleValidator.class)
public @interface ValidRole {

    String message() default "角色编码无效";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
