package interview.ai.llm.client;

import interview.ai.llm.model.RouteResult;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * LLM 客户端工厂：根据路由结果创建 ChatModel
 */

@Component
public class LlmClientFactory {



    public ChatModel createChatModel(RouteResult routeResult){
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(routeResult.getTimeoutSeconds()));

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(routeResult.getBaseUrl())
                .apiKey(routeResult.getApiKey())
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();

        BigDecimal temperature = routeResult.getTemperature();
        BigDecimal topP = routeResult.getTopP();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(routeResult.getModel())
                .temperature(temperature != null ? temperature.doubleValue() : 0.7)
                .topP(topP != null ? topP.doubleValue() : 0.9)
                .maxTokens(routeResult.getMaxOutputTokens())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api).defaultOptions(options)
                .build();
    }

}
