package interview.anticheat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.anticheat.model.entity.AntiCheatLog;

/**
 * @author zhuxi
 * @apiNote 防作弊检测日志表 Mapper（简单查询一律 LambdaQuery，无手写 SQL）
 */
public interface AntiCheatLogMapper extends BaseMapper<AntiCheatLog> {
}
