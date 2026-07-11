package interview.framework.annonate;

import interview.common.enums.RiskLevel;

import java.lang.annotation.*;

/**
 * @author zhuxi
 */

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MaxRiskLevel {
    /**
     * 允许访问该接口的最大风险等级
     * 默认 RiskLevel.NO_RISK,只要 > 0 就立马拦截风控
     */
    RiskLevel value() default RiskLevel.NO_RISK;
}
