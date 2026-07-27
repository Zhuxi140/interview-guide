package interview.voiceinterview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.voiceinterview.model.entity.VoiceInterviewSession;
import interview.voiceinterview.model.req.EndSessionReq;
import interview.voiceinterview.model.vo.EndSessionVO;
import interview.voiceinterview.model.vo.JoinTokenVO;
import interview.voiceinterview.model.vo.SessionDetailVO;

public interface VoiceInterviewSessionService extends IService<VoiceInterviewSession> {

    /**
     * 生成语音面试连接凭证
     * @param scheduleId 排期ID
     * @return 连接凭证及会话信息
     */
    JoinTokenVO generateJoinToken(Long scheduleId);

    /**
     * 查询会话详情（含语音扩展信息）
     * @param sessionId 会话ID
     * @return 会话详情
     */
    SessionDetailVO getSessionWithVoice(Long sessionId);

    /**
     * 幂等结束语音会话
     * @param sessionId 会话ID
     * @param req 结束请求
     * @return 结束结果
     */
    EndSessionVO endSession(Long sessionId, EndSessionReq req);
}
