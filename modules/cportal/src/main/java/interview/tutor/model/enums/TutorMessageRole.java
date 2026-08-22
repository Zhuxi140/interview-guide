package interview.tutor.model.enums;

/**
 * 答疑消息展示角色，由表内 message_type（USER/AI）映射。
 */
public enum TutorMessageRole {

    /**
     * 求职者提问。
     */
    USER,

    /**
     * AI 回答。
     */
    ASSISTANT;

    /**
     * 将存储的消息类型转换为展示角色。
     *
     * @param messageType 存储消息类型（USER / AI）
     * @return 展示角色
     */
    public static TutorMessageRole fromMessageType(String messageType) {
        return "AI".equals(messageType) ? ASSISTANT : USER;
    }
}
