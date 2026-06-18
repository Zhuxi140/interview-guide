package interview.common.enums;



/**
 * @author zhuxi
 * @apiNote 角色作用域枚举
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
