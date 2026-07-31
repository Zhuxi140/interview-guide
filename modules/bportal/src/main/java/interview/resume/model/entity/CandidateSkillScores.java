package interview.resume.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.common.enums.CandidateDimensionCode;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 人才画像固定维度评分明细。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "candidate_skill_scores", autoResultMap = true)
public class CandidateSkillScores implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long candidateProfileId;

    private CandidateDimensionCode dimensionCode;

    private Integer score;

    private String aiJustification;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String evidenceJson;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
