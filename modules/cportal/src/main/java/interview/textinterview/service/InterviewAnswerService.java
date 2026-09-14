package interview.textinterview.service;

import interview.textinterview.model.req.InterviewAnswerSubmitReq;
import interview.textinterview.model.vo.InterviewAnswerSubmitVO;

/**
 * 候选人作答提交服务（U3-08）。
 *
 * <p>保存候选人作答并触发评分、追问生成或会话结束判定。
 * 该用例是协议无关的：REST 端点与未来的 WebSocket 消息处理器都复用本服务，
 * 身份信息统一从 {@code AuthContext} 读取（WS 握手时已绑定候选人身份）。</p>
 *
 * @author zhuxi
 */
public interface InterviewAnswerService {

    /**
     * 提交当前待答题目的作答。
     *
     * <p>作答必须对应服务端定位的"当前待答题目"（从会话时间线的
     * {@code question.completed} 事件解析），不接受客户端上报题目信息，
     * 防止伪造题目。幂等键命中时重放既有结果。</p>
     *
     * @param sessionId 会话 ID
     * @param req       作答内容与幂等键
     * @return 作答结果（含评分占位、追问/结束标记）
     */
    InterviewAnswerSubmitVO submitAnswer(Long sessionId, InterviewAnswerSubmitReq req);
}
