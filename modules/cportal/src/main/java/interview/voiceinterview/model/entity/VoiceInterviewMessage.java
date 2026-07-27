package interview.voiceinterview.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.voiceinterview.model.enums.VoiceInterviewPhase;
import interview.voiceinterview.model.enums.VoiceMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "voice_interview_messages")
public class VoiceInterviewMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long interviewSessionId;

    private Long enterpriseId;

    private String eventId;

    private VoiceMessageType messageType;

    private VoiceInterviewPhase currentPhase;

    private Long parentMessageId;

    @Builder.Default
    private Integer followUpDepth = 0;

    private String asrText;

    private String llmResponseText;

    private Long sequenceNum;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    private String traceId;

    @TableLogic
    @Builder.Default
    private Boolean isDeleted = false;
}
