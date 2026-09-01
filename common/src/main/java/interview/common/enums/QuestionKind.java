package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 面试题目类型枚举
 */
@Getter
@AllArgsConstructor
public enum QuestionKind {

    /**
     * 阶段开场首题
     */
    FIRST("开场首题"),

    /**
     * 基于上一题作答的深入追问
     */
    FOLLOW_UP("追问"),

    /**
     * 真人面试官插话或接管出题
     */
    INTERVIEWER("面试官插话");

    private final String description;
}
