package interview.candidate.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.candidate.model.enums.CandidateBasicProfileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("candidate_profiles")
public class CandidateBasicProfile implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String expectedCity;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String education;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;

    @Builder.Default
    private CandidateBasicProfileStatus profileStatus = CandidateBasicProfileStatus.INCOMPLETE;

    @Version
    @Builder.Default
    private Integer version = 0;

    @TableLogic
    @Builder.Default
    private Boolean isDeleted = false;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
