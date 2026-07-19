package interview.common.constant;

import interview.common.enums.MsgTopic;
import lombok.Builder;

@Builder
public record Message(MsgTopic topic, String payload) {}
