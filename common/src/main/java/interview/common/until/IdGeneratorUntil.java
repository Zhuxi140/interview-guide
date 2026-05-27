package interview.common.until;

import cn.hutool.core.util.IdUtil;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote Id生成工具类
 */

public class IdGeneratorUntil {

    /**
     * 生成id
     * @param workerId 工作机器ID
     * @param datacenterId 数据中心ID
     * @return id(Long)
     */
    public static Long generateId(long workerId, long datacenterId){
        return IdUtil.getSnowflake(workerId, datacenterId).nextId();
    }

    /**
     * 生成traceId
     * @return traceId(String)
     */
    public static String generateTraceId(){
        return IdUtil.fastSimpleUUID();
    }
}
