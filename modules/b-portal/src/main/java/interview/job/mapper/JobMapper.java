package interview.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.job.model.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author zhuxi
 */

@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
