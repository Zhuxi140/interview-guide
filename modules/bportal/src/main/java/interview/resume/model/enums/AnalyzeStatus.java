package interview.resume.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 */

@Getter
@AllArgsConstructor
public enum AnalyzeStatus {
    UPLOADING("文件上传中"),
    PENDING("待AI解析"),
    PROCESSING("AI解析中"),
    COMPLETED("AI解析成功"),
    FAILED("AI解析失败"),
    UPLOAD_FAILED("上传失败");

    private final String msg;
}
