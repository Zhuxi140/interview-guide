package interview.ai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestController.class)
@ImportAutoConfiguration({OpenAiChatAutoConfiguration.class, ChatClientAutoConfiguration.class, ToolCallingAutoConfiguration.class})
class TestControllerTest {

    @Autowired
    private TestController controller;

    @Test
    void testChatWithModel() {
        String response = controller.test("用一句话介绍你自己");
        System.out.println("AI 回复: " + response);
        assertThat(response).isNotBlank();
    }

}
