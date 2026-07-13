package interview.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EducationLevel {

    HIGH_SCHOOL(0, "高中"),
    ASSOCIATE(1, "大专"),
    BACHELOR(2, "本科"),
    MASTER(3, "硕士"),
    PHD(4, "博士");

    @EnumValue
    private final Integer code;
    private final String desc;

    public static EducationLevel fromCode(Integer code) {
        for (EducationLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}
