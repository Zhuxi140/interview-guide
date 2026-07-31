package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HR AI 初筛建议，不代表最终投递状态。
 */
@Getter
@AllArgsConstructor
public enum ScreeningRecommendation {
    RECOMMEND_PASS("建议通过"),
    RECOMMEND_REJECT("建议淘汰");

    private final String message;
}
