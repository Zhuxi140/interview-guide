package interview.voiceinterview.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.voiceinterview.model.enums.VoiceInterviewPhase;
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
@TableName(value = "voice_interview_sessions")
public class VoiceInterviewSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long interviewSessionId;

    private String skillCode;

    @Builder.Default
    private VoiceInterviewPhase currentPhase = VoiceInterviewPhase.INTRO;

    @Builder.Default
    private Integer actualDurationSeconds = 0;

    private String recordingObjectKey;

    private OffsetDateTime recordingConsentAt;

    private OffsetDateTime retentionUntil;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;
}
