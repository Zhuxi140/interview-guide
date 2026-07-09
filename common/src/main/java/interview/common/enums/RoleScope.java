package interview.common.enums;


import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 角色作用域枚举
 */

@Getter
public enum RoleScope {
    PLATFORM,
    ENTERPRISE,
    USER,
    UNKNOWN;

    public static RoleScope getRoleScope(String roleScope){
        for (RoleScope value : RoleScope.values()) {
            if (value.name().equalsIgnoreCase(roleScope)){
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid roleScope: " + roleScope);
    }
}
