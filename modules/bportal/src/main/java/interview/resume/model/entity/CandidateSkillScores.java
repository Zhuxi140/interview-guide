package interview.resume.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 标准化人才画像维度打分表
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("candidate_skill_scores")
public class CandidateSkillScores implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * [逻辑外键]→resume_analyses
     */
    private Long resumeAnalysisId;

    /**
     * 打分维度编码
     */
    private String dimensionCode;

    /**
     * 单项得分 (0-100)
     */
    private Integer score;

    /**
     * 大模型针对该维度给出扣分或得分的推导依据
     */
    private String aiJustification;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
