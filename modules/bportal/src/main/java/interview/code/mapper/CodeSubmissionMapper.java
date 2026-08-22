package interview.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.code.model.entity.CodeSubmission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代码提交记录 Mapper。
 */
@Mapper
public interface CodeSubmissionMapper extends BaseMapper<CodeSubmission> {
}
