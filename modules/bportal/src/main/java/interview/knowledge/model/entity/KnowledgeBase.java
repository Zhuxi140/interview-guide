package interview.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import interview.knowledge.model.enums.KnowledgeVisibility;
import interview.knowledge.model.enums.VectorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 知识库文档实体，对应表 knowledge_bases。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "knowledge_bases", autoResultMap = true)
public class KnowledgeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** GLOBAL 为平台公共底座；PRIVATE 为企业私有文档 */
    private KnowledgeVisibility visibility;

    /** 归属企业租户 ID，GLOBAL 级别为 0 */
    private Long enterpriseId;

    /** SHA-256 文件哈希，按 (enterprise_id, file_hash) 防重 */
    private String fileHash;

    /** 原始文件名（仅展示，不参与对象键拼接） */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 知识库名称 */
    private String name;

    /** 向量化状态 (PENDING / PROCESSING / COMPLETED / FAILED) */
    private VectorStatus vectorStatus;

    /** 最近一次向量化失败原因，成功后清空 */
    private String vectorError;

    /** 向量分块数量冗余列（document_chunks 计数） */
    private Integer chunkCount;

    @Version
    private Integer version;

    /** 上传时间 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime uploadedAt;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
