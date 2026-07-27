package interview.voiceinterview.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VoiceInterviewPhase {

    INTRO("介绍"),
    TECH("技术"),
    PROJECT("项目"),
    HR("人事");

    private final String message;
}
