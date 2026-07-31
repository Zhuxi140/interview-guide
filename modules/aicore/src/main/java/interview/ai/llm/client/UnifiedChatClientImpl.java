package interview.ai.llm.client;

import interview.ai.llm.model.AiResult;
import interview.ai.llm.model.LlmConfigSnapshot;
import interview.ai.llm.model.RouteResult;
import interview.ai.llm.router.AiRouteService;
import interview.common.enums.AiSceneCode;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedChatClientImpl implements UnifiedChatClient {

    private static final int PROMPT_OVERHEAD_TOKENS = 64;

    private final AiRouteService routeService;
    private final LlmClientFactory factory;
    private final TokenCountEstimator tokenEstimator = new JTokkitTokenCountEstimator();

    @Override
    public <T> AiResult<T> callStructured(AiSceneCode sceneCode, String userPrompt, Class<T> responseType) {
        // 解析路由和版本化系统提示词，并在模型调用前检查输入预算。
        RouteResult route = routeService.route(sceneCode);
        String systemPrompt = getSystemPrompt(sceneCode, route.getPromptVersion());
        checkInputTokens(route, systemPrompt, userPrompt);
        ChatModel chatModel = factory.createChatModel(route);

        // 执行结构化调用，并为业务持久化返回不含密钥的配置快照。
        T entity = ChatClient.builder(chatModel).build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(responseType);
        return new AiResult<>(
                entity,
                LlmConfigSnapshot.from(route, systemPrompt)
        );
    }

    @Override
    public AiResult<String> callText(AiSceneCode sceneCode, String userPrompt) {
        // 文本调用与结构化调用共用路由、提示词和 Token 限制。
        RouteResult route = routeService.route(sceneCode);
        String systemPrompt = getSystemPrompt(sceneCode, route.getPromptVersion());
        checkInputTokens(route, systemPrompt, userPrompt);
        ChatModel chatModel = factory.createChatModel(route);

        String content = ChatClient.builder(chatModel).build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        return new AiResult<>(
                content,
                LlmConfigSnapshot.from(route, systemPrompt)
        );
    }

    private void checkInputTokens(RouteResult route,
                                  String systemPrompt,
                                  String userPrompt) {
        int estimatedTokens =
                tokenEstimator.estimate(systemPrompt)
                        + tokenEstimator.estimate(userPrompt)
                        + PROMPT_OVERHEAD_TOKENS;

        if (estimatedTokens > route.getMaxInputTokens()) {
            throw new BusinessException(ErrorCode.AI_TOKEN_LIMIT_EXCEEDED);
        }
    }

    private String getSystemPrompt(AiSceneCode sceneCode, String promptVersion) {
        if (promptVersion == null || !promptVersion.matches("v\\d+")) {
            throw new BusinessException(
                    ErrorCode.AI_SCENE_PARAM_UNSUPPORTED
            );
        }
        String promptPath = sceneCode.getPromptPath();

        String path = promptPath + promptVersion + "/system.txt";
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.error("系统提示词文件不存在。path:{}", path);
            throw new BusinessException(ErrorCode.AI_SCENE_PARAM_UNSUPPORTED);
        }

        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取系统提示词文件异常。path={}", path, e);
            throw new BusinessException(ErrorCode.AI_SCENE_PARAM_UNSUPPORTED);
        }
    }
}
