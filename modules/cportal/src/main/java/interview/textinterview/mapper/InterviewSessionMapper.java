package interview.textinterview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.textinterview.model.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {
}
