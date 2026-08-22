package interview.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.code.model.entity.CodeSubmissionResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 逐测试用例执行结果 Mapper。
 */
@Mapper
public interface CodeSubmissionResultMapper extends BaseMapper<CodeSubmissionResult> {
}
