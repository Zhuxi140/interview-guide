package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizType {

    RESUME_ANALYSIS("简历解析"),
    TEXT_INTERVIEW("文本面试"),
    VOICE("语音面试"),
    CODE("代码沙箱"),
    RAG("RAG 检索"),
    TUTOR("智能导师");

    private final String message;
}
