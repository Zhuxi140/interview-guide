package interview.framework.annonate;

import interview.common.enums.Logic;
import interview.common.enums.PermissionScope;
import interview.common.enums.RoleScope;

import java.lang.annotation.*;

/**
 * @author zhuxi
 */

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     *  所需权限标识
     */
    String[] permissions();

    /**
     * 指定该权限校验的作用域，默认为PLATFORM
     * 即检验Sys_user_role
     * PermissionScope.ENTERPRISE 则效验企业权限，即EnterprisesTeamMembers的RoleId
     * PermissionScope.PLATFORM 同默认
     */
    PermissionScope scope() default PermissionScope.PLATFORM;

    /**
     * 逻辑选择 控制多个权限之间的关系
     * AND : 所需权限都拥有，通过
     * OR  : 所需权限中拥有任一一个，通过
     */
    Logic logic() default Logic.AND;
}
