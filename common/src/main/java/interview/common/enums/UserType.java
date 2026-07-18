package interview.common.enums;

/**
 * @author zhuxi
 * @apiNote 用户类型枚举类
 */

public enum UserType {
    HR,
    CANDIDATE,
    PLATFORM_ADMIN,
    PLATFORM_OPS;

    /**
     * 根据名称获取枚举类
     * @param value 字符串
     * @return 枚举类
     */
    public static UserType fromString(String value) {
        for (UserType type : UserType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid UserType: " + value);
    }
}
