package interview.textinterview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.common.constant.InterviewConnectionContext;
import interview.textinterview.model.entity.InterviewSession;
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
     * 原子校验并消费一次性连接凭证（仅允许消费一次）
     * @param token 连接凭证
     * @return 消费成功后凭证携带的上下文；过期、不存在或已消费时返回 null
     */
    InterviewConnectionContext consumeConnectionToken(String token);

    /**
     * 查询面试会话状态
     * @param sessionId 会话ID
     * @return 会话状态
     */
    InterviewSessionVO getSession(Long sessionId);

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
