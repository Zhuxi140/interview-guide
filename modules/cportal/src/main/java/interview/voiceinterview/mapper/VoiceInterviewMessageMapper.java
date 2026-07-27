package interview.voiceinterview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.voiceinterview.model.entity.VoiceInterviewMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VoiceInterviewMessageMapper extends BaseMapper<VoiceInterviewMessage> {
}
