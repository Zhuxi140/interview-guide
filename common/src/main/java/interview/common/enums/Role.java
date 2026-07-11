package interview.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 角色枚举
 */

@AllArgsConstructor
@Getter
public enum Role {

    SUPER_ADMIN(1001,"超级管理员",RoleScope.PLATFORM,PermissionScope.PLATFORM),
    FINANCE_ADMIN(1002,"财务管理员",RoleScope.PLATFORM,PermissionScope.PLATFORM),
    ENTERPRISE_OWNER(2001,"企业所有者",RoleScope.ENTERPRISE,PermissionScope.ENTERPRISE),
    ENTERPRISE_ADMIN(2002,"企业管理员",RoleScope.ENTERPRISE,PermissionScope.ENTERPRISE),
    HR_MANAGER(2003,"HR经理",RoleScope.ENTERPRISE,PermissionScope.ENTERPRISE),
    HR_RECRUITER(2004,"招聘专员",RoleScope.ENTERPRISE,PermissionScope.ENTERPRISE),
    INTERVIEWER(2005,"面试官",RoleScope.ENTERPRISE,PermissionScope.ENTERPRISE),
    CANDIDATE(3001,"求职者",RoleScope.USER,PermissionScope.PLATFORM),
    UNKNOWN(4001,"未知",RoleScope.UNKNOWN,PermissionScope.UNKNOWN);

    @EnumValue
    private final Integer code;
    private final String msg;
    private final RoleScope roleScope;
    private final PermissionScope permissionScope;


    /**
     * @param value 枚举值
     * @return Role
     */
    public static Role fromString(String value) {
        for (Role type : Role.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid Role: " + value);
    }

    /**
     * @param code 枚举值
     * @return Role
     */
    public static Role fromCode(Integer code) {
        for (Role type : Role.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid Role: " + code);
    }

    public static Integer getCodeFilterScope(PermissionScope permissionScope,Role role) {
            if (role.permissionScope.equals(permissionScope)) {
                return role.code;
            }
            return null;
    }
}
