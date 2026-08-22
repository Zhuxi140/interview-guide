package interview.rag.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 面试模板-知识库关联实体，对应表 interview_template_knowledge_bases。
 *
 * <p>第七阶段新增关联表：表设计无独立模板绑定表，
 * 且不修改既有 interview_stage_templates 结构。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "interview_template_knowledge_bases", autoResultMap = true)
public class InterviewTemplateKnowledgeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 逻辑关联 interview_stage_templates.id */
    private Long templateId;

    /** 逻辑关联 knowledge_bases.id */
    private Long knowledgeBaseId;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;
}
