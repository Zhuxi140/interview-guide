package interview.textinterview.service;

import interview.textinterview.model.req.InterviewTakeoverEndReq;
import interview.textinterview.model.req.InterviewTakeoverStartReq;
import interview.textinterview.model.vo.InterviewTakeoverEndVO;
import interview.textinterview.model.vo.InterviewTakeoverStartVO;

/**
 * 面试官人工接管服务。
 */
public interface InterviewTakeoverService {

    /**
     * 开始人工接管面试会话
     * @param enterpriseId 企业ID
     * @param sessionId 会话ID
     * @param idempotencyKey 幂等键
     * @param req 接管请求
     * @return 接管结果
     */
    InterviewTakeoverStartVO startTakeover(Long enterpriseId,
                                            Long sessionId,
                                            String idempotencyKey,
                                            InterviewTakeoverStartReq req);

    /**
     * 结束人工接管
     * @param enterpriseId 企业ID
     * @param sessionId 会话ID
     * @param takeoverId 接管记录ID
     * @param req 结束请求
     * @return 结束结果
     */
    InterviewTakeoverEndVO endTakeover(Long enterpriseId,
                                        Long sessionId,
                                        Long takeoverId,
                                        InterviewTakeoverEndReq req);
}
