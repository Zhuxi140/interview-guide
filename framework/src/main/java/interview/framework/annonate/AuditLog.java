package interview.framework.annonate;


import java.lang.annotation.*;

/**
 * @author zhuxi
 * @apiNote 审计日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

}
