package interview.voiceinterview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.voiceinterview.model.entity.VoiceInterviewMessage;
import interview.voiceinterview.model.vo.VoiceMessagePageVO;

public interface VoiceInterviewMessageService extends IService<VoiceInterviewMessage> {

    /**
     * 游标查询语音消息明细
     * @param sessionId 会话ID
     * @param afterSequence 起始事件序号（不含），默认 0
     * @param size 每页条数，默认 50，最大 100
     * @return 语音消息分页
     */
    VoiceMessagePageVO queryMessages(Long sessionId, Long afterSequence, Integer size);
}
