package interview.system.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.auth.model.entity.UserToken;

/**
 * @author zhuxi
 * @apiNote 认证服务 Mapper
 * @since 2026/5/27 14:05
 */
public interface AuthMapper extends BaseMapper<UserToken> {
}
