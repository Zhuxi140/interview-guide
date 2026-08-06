package interview.voiceinterview.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.voiceinterview.mapper.VoiceInterviewSessionMapper;
import interview.voiceinterview.model.entity.VoiceInterviewSession;
import interview.voiceinterview.service.VoiceInterviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 语音面试会话实现。
 *
 * <p>语音会话入口复用统一面试会话链路（join-token / getSession / endSession），
 * 本服务仅保留语音扩展记录的数据能力，会话主流程见文本模块统一会话服务。</p>
 */
@Service
@RequiredArgsConstructor
public class VoiceInterviewSessionServiceImpl extends ServiceImpl<VoiceInterviewSessionMapper, VoiceInterviewSession>
        implements VoiceInterviewSessionService {
}
