package interview.common.Enum;



/**
 * @author zhuxi
 * @apiNote 角色作用域枚举
 * @since 2026/5/26 14:09
 */

public enum RoleScope {
    PLATFORM,
    ENTERPRISE,
    USER;

    public static RoleScope getRoleScope(String roleScope){
        for (RoleScope value : RoleScope.values()) {
            if (value.name().equalsIgnoreCase(roleScope)){
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid roleScope: " + roleScope);
    }
}
