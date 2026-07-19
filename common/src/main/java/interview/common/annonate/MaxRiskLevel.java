package interview.common.annonate;

import interview.common.enums.RiskLevel;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MaxRiskLevel {
    RiskLevel value() default RiskLevel.NO_RISK;
}
