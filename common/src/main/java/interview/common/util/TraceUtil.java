package interview.common.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.MDC;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 链路追踪工具类
 * <P>
 *     提供操作traceId的方法对接第三方、以及为定时任务生成独立追踪traceId
 * </P>
 */
public class TraceUtil {

    private static final String TRACE_ID = "X-Trace-Id";

    /**
     * 获取traceId
     * @return traceId
     * <p>
     *     如果获取不到，则返回空字符串
     * </p>
     */
    public static String getTraceId(){
        String traceId = MDC.get(TRACE_ID);

        // 极端情况下，为防止因traceId为空或无效导致 前端判断出错、后续流程追踪丢失等情况
        if (!StrUtil.isNotBlank(traceId)){
            // 兜底生成traceId，并写入MDC
            traceId = IdUtil.fastSimpleUUID();
            MDC.put(TRACE_ID, traceId);
        }

        return traceId;
    }

    /**
     * 初始化定时任务线程的traceId
     */
    public static void initScheduledTrace(){
        MDC.put(TRACE_ID, IdGeneratorUtil.generateTraceId());
    }


    /**
     * 清理traceId
     */
    public static void clearTraceId(){
        MDC.remove(TRACE_ID);
    }
}
