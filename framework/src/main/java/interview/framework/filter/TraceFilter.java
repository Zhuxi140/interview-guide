package interview.framework.filter;

import cn.hutool.core.util.StrUtil;
import interview.common.util.IdGeneratorUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 调用链追踪过滤器
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter implements Filter {

    private static final String TRACE_ID = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String traceId = request.getHeader("X-Trace-Id");

        // 兜底生成TraceId，避免消息头中缺少TraceId而导致无法追踪
        if (!StrUtil.isNotBlank(traceId)){
            traceId = IdGeneratorUtil.generateTraceId();
        }


        MDC.put("traceId", traceId);
        response.setHeader(TRACE_ID, traceId);

        try {
            filterChain.doFilter(request,response);
        }finally {
            // 清除MDC中的traceId，防止线程污染
            MDC.remove(TRACE_ID);
        }

    }

}
