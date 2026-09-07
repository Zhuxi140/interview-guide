package interview.textinterview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import interview.common.constant.InterviewConnectionContext;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.model.req.InterviewSessionEndReq;
import interview.textinterview.model.vo.InterviewAnswerListItemVO;
import interview.textinterview.model.vo.InterviewCurrentQuestionVO;
import interview.textinterview.model.vo.InterviewJoinTokenVO;
import interview.textinterview.model.vo.InterviewSessionEndVO;
import interview.textinterview.model.vo.InterviewSessionReadyVO;
import interview.textinterview.model.vo.InterviewSessionVO;
import interview.textinterview.model.vo.InterviewTimelinePageVO;

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

    /**
     * 查询当前待作答题目
     * @param sessionId 会话 ID
     * @return 当前题目；尚未生成下一题时返回 pending=true
     */
    InterviewCurrentQuestionVO getCurrentQuestion(Long sessionId);

    /**
     * 查询会话内本人已提交作答（REST 兜底：断线后恢复现场）
     * @param sessionId 会话ID
     * @param page 页码（从 1 开始）
     * @param size 每页条数（1~100）
     * @return 作答分页
     */
    IPage<InterviewAnswerListItemVO> pageAnswers(Long sessionId, Integer page, Integer size);

    /**
     * 候选人就绪，原子推进会话与排期状态并触发生成首题（REST 兜底：等价 WS client.ready）
     *
     * @param sessionId 会话 ID
     * @param idempotencyKey 幂等键
     * @return 就绪响应
     */
    InterviewSessionReadyVO readySession(Long sessionId, String idempotencyKey);

}
