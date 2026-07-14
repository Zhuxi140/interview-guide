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
 * 简历 AI 分析结果表
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */


@Data
@Builder
@TableName("resume_analyses")
public class ResumeAnalyses implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * [逻辑外键]→enterprises
     */
    private Long enterpriseId;

    /**
     * [逻辑外键]→resumes
     */
    private Long resumeId;

    /**
     * AI 综合评分 (0-100)
     */
    private Integer overallScore;

    /**
     * 优点列表 (JSON)
     */
    private Object strengthsJson;

    /**
     * 改进建议 (JSON)
     */
    private Object suggestionsJson;

    /**
     * 评测时间
     */
    private LocalDateTime analyzedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 调用链 ID（追溯大模型响应）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
