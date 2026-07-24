package interview.interviewcfg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.interviewcfg.model.entity.WorkflowTransitionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowTransitionLogMapper extends BaseMapper<WorkflowTransitionLog> {
}
