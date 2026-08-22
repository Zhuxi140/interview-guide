package interview.compliance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.compliance.model.entity.UserApiPolicy;

/**
 * @author zhuxi
 * @apiNote 用户 API 策略表 Mapper（简单查询一律 LambdaQuery，无手写 SQL）
 */
public interface UserApiPolicyMapper extends BaseMapper<UserApiPolicy> {
}
