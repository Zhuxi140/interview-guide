package interview.interviewcfg.model.entity;

import com.baomidou.mybatisplus.annotation.*;
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
@TableName(value = "interview_phase_configs", autoResultMap = true)
public class InterviewPhaseConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long templateId;

    private String phaseCode;

    @Builder.Default
    private Integer questionCount = 3;

    @Builder.Default
    private Double difficultyWeight = 0.5;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String promptOverride;

    @Version
    private Integer version;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
