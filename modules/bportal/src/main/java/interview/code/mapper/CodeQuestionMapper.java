package interview.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.code.model.entity.CodeQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 编程题库 Mapper。
 */
@Mapper
public interface CodeQuestionMapper extends BaseMapper<CodeQuestion> {
}
