package interview.voiceinterview.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VoiceMessageType {

    USER_SPEECH("用户语音"),
    AI_SPEECH("AI回复"),
    SYSTEM("系统消息");

    private final String message;
}
