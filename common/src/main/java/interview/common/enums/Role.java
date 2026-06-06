package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 角色枚举
 * @since 2026/5/26 11:05
 */

@AllArgsConstructor
@Getter
public enum Role {

    SUPER_ADMIN(1001,"超级管理员"),
    FINANCE_ADMIN(1002,"财务管理员"),
    ENTERPRISE_OWNER(2001,"企业所有者"),
    ENTERPRISE_ADMIN(2002,"企业管理员"),
    HR_MANAGER(2003,"HR经理"),
    HR_RECRUITER(2004,"招聘专员"),
    INTERVIEWER(2005,"面试官"),
    CANDIDATE(3001,"求职者");

    private final Integer code;
    private final String msg;


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
}
