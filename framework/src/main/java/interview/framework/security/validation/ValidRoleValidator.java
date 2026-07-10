package interview.framework.security.validation;

import interview.common.enums.Role;
import interview.framework.annonate.ValidRole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author zhuxi
 */
public class ValidRoleValidator implements ConstraintValidator<ValidRole, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            Role.fromCode(value);
            return true;
        } catch (IllegalArgumentException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "角色编码 " + value + " 无效，可用值: "
                    + buildValidValues()
            ).addConstraintViolation();
            return false;
        }
    }

    private String buildValidValues() {
        StringBuilder sb = new StringBuilder();
        for (Role role : Role.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(role.getCode()).append("=").append(role.name());
        }
        return sb.toString();
    }
}
