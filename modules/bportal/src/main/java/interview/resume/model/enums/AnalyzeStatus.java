package interview.resume.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 */

@Getter
@AllArgsConstructor
public enum AnalyzeStatus {
    PENDING("待解析"),
    PROCESSING("解析中"),
    COMPLETED("解析成功"),
    FAILED("解析失败");

    private final String msg;
}
