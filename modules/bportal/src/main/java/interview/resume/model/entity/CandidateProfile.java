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
 * 候选人画像聚合表（供雷达图读取）
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */

@Data
@Builder
@TableName("candidate_profile")
public class CandidateProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * [逻辑外键]→sys_users, 仅 user_type='CANDIDATE'
     */
    private Long userId;

    /**
     * 打分维度编码
     */
    private String dimensionCode;

    /**
     * 各版简历该维度的平均分
     */
    private Integer avgScore;

    /**
     * 最新简历的 AI 推导依据
     */
    private String latestJustification;

    /**
     * 逻辑删除标识
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
