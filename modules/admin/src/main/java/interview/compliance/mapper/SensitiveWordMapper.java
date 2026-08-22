package interview.compliance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.compliance.model.entity.SensitiveWord;

/**
 * @author zhuxi
 * @apiNote 敏感词表 Mapper（简单查询一律 LambdaQuery，无手写 SQL）
 */
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
}
