package interview.textinterview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.model.req.InterviewAnswerSubmitReq;
import interview.textinterview.model.req.InterviewSessionCreateReq;
import interview.textinterview.model.req.InterviewSessionEndReq;
import interview.textinterview.model.vo.*;

public interface InterviewSessionService extends IService<InterviewSession> {

    /**
     * 创建或复用排期对应的统一面试会话并签发连接凭证
     * @param scheduleId 排期ID
     * @param idempotencyKey 幂等键
     * @return 连接凭证及会话信息
     */
    InterviewJoinTokenVO generateJoinToken(Long scheduleId, String idempotencyKey);

    /**
     * 候选人在收到面试邀请后，选择开始面试，幂等创建文本面试会话
     * @param req 创建请求
     * @return 创建结果（含首题）
     */
    InterviewSessionCreateVO createSession(InterviewSessionCreateReq req);

    /**
     * 查询面试会话状态
     * @param sessionId 会话ID
     * @return 会话状态
     */
    InterviewSessionVO getSession(Long sessionId);

    /**
     * 候选人幂等提交当前题答案
     * @param sessionId 会话ID
     * @param req 提交请求
     * @return 提交结果（含AI评分、追问或下一题）
     */
    InterviewAnswerSubmitVO submitAnswer(Long sessionId, InterviewAnswerSubmitReq req);

    /**
     * 游标查询面试问答记录
     * @param sessionId 会话ID
     * @param cursor 游标
     * @param size 每页条数
     * @return 历史记录
     */
    IPage<InterviewHistoryItemVO> getHistory(Long sessionId, Long cursor, Integer size);

    /**
     * 幂等结束面试
     * @param sessionId 会话ID
     * @param req 结束请求
     * @return 结束结果
     */
    InterviewSessionEndVO endSession(Long sessionId, InterviewSessionEndReq req);

    /**
     * 按序号增量查询可回放语义事件
     * @param sessionId 会话ID
     * @param afterSequence 起始序号（不含）
     * @param size 返回条数
     * @return 时间线分页
     */
    InterviewTimelinePageVO getTimeline(Long sessionId, Long afterSequence, Integer size);
}
