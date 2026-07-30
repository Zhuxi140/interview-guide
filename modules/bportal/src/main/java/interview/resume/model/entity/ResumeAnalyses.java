package interview.resume.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "resume_analyses", autoResultMap = true)
public class ResumeAnalyses implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

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
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String strengthsJson;

    /**
     * 改进建议 (JSON)
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String suggestionsJson;

    /**
     * LLM 配置快照 (JSON)
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String llmConfigSnapshot;

    /**
     * 评测时间
     */
    private OffsetDateTime analyzedAt;

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
    private OffsetDateTime createdAt;
}
