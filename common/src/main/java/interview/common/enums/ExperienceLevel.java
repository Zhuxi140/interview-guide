package interview.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExperienceLevel {

    FRESHMAN(0, "应届"),
    ONE_TO_THREE(1, "1-3年"),
    THREE_TO_FIVE(2, "3-5年"),
    FIVE_TO_TEN(3, "5-10年"),
    TEN_PLUS(4, "10年以上");

    @EnumValue
    private final Integer code;
    private final String desc;

    public static ExperienceLevel fromCode(Integer code) {
        for (ExperienceLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}
