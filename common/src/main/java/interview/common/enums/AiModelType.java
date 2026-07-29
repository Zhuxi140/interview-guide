package interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiModelType {

    CHAT("chat"),
    EMBEDDING("embedding"),
    ASR("asr"),
    TTS("tts");

    private final String type;
}
