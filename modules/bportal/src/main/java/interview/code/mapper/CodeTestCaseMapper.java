package interview.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.code.model.entity.CodeTestCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 编程题测试用例 Mapper。
 */
@Mapper
public interface CodeTestCaseMapper extends BaseMapper<CodeTestCase> {
}
