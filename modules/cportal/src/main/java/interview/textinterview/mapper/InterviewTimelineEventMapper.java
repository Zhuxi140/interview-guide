package interview.textinterview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.textinterview.model.entity.InterviewTimelineEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewTimelineEventMapper extends BaseMapper<InterviewTimelineEvent> {
}
