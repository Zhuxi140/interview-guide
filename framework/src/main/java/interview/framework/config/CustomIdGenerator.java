package interview.framework.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import interview.common.util.IdGeneratorUtil;
import org.springframework.stereotype.Component;

/**
 * @author zhuxi
 * @apiNote 自定义ID生成器
 */
@Component
public class CustomIdGenerator implements IdentifierGenerator {

    // 单体阶段固定为 1用于过渡，未来拓展微服务/分布式 时从 Nacos / Redis 读取
    private final long workerId = 1L;
    private final long datacenterId = 1L;

    @Override
    public Number nextId(Object entity) {
        return IdGeneratorUtil.generateId(workerId,datacenterId);
    }
}
