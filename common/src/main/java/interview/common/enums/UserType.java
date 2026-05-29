package interview.common.enums;

/**
 * @author zhuxi
 * @apiNote 用户类型枚举类
 * @since 2026-05-26
 */

public enum UserType {
    HR,
    CANDIDATE,
    PLATFORM_ADMIN;

    public static UserType fromString(String value) {
        for (UserType type : UserType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid UserType: " + value);
    }
}
