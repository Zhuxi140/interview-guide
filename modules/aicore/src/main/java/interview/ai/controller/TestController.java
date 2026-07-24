package interview.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final ChatClient.Builder chatClientBuilder;

    @GetMapping("/test")
    public String test(@RequestParam String msg) {
        return chatClientBuilder.build().prompt().user(msg).call().content();
    }
}
