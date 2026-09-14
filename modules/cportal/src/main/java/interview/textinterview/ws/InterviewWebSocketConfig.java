package interview.textinterview.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 面试实时通道端点注册（U3-01）。
 *
 * <p>统一端点 {@value #INTERVIEW_WS_PATH}：文本面试与语音面试共用同一入口，
 * 差异体现在下行事件类型（语音的 {@code asr.*} / {@code webrtc.*} 见第五阶段接口文档），
 * 不另开端点。</p>
 *
 * @author zhuxi
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class InterviewWebSocketConfig implements WebSocketConfigurer {

    /** 统一实时通道路径：会话 ID 作为路径变量，握手拦截器据此与凭证做一致性校验。 */
    public static final String INTERVIEW_WS_PATH = "/ws/v1/interview-sessions/{sessionId}";

    private final InterviewWebSocketHandler interviewWebSocketHandler;
    private final InterviewHandshakeInterceptor interviewHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interviewWebSocketHandler, INTERVIEW_WS_PATH)
                .addInterceptors(interviewHandshakeInterceptor);

        // 此处刻意不调用 setAllowedOrigins：保持 Spring 默认的同源策略。
        // TODO(你来实现) 若前端不经同源代理、而是从独立域名/端口直连本端点，
        //   需要显式声明 setAllowedOriginPatterns 并决定是否维护 Origin 白名单。
        //   注意：放宽来源等于放弃跨站连接防护——一次性凭证能挡住未授权连接，
        //   但挡不住「已授权用户的浏览器被第三方页面驱动连接」这一类滥用。
    }
}
