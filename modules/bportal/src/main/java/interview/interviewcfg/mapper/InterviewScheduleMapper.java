package interview.interviewcfg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.interviewcfg.model.entity.InterviewSchedule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewScheduleMapper extends BaseMapper<InterviewSchedule> {
}
