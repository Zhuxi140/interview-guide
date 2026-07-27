package interview.voiceinterview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.voiceinterview.model.entity.VoiceInterviewEvaluation;
import interview.voiceinterview.model.vo.EvaluationVO;

public interface VoiceInterviewEvaluationService extends IService<VoiceInterviewEvaluation> {

    /**
     * 查询语音评估结果
     * @param sessionId 会话ID
     * @return 评估结果
     */
    EvaluationVO getEvaluation(Long sessionId);
}
