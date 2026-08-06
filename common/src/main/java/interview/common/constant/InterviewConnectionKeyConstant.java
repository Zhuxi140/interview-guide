package interview.common.constant;

/**
 * 面试会话连接凭证 Redis Key 常量。
 *
 * @author zhuxi
 */
public interface InterviewConnectionKeyConstant {

    String PREFIX = "interview:connection:token";

    /**
     * 获取一次性连接凭证 key
     * @param token 连接凭证
     * @return Redis key
     */
    static String getConnectionKey(String token) {
        return PREFIX + ":" + token;
    }
}