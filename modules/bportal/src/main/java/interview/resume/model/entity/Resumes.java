package interview.resume.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import interview.resume.model.enums.AnalyzeStatus;
import lombok.*;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * <p>
 * 简历底座表
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resumes implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * [逻辑外键]→sys_users, 候选人用户 ID
     */
    private Long userId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * pdf / doc / docx
     */
    private String fileType;

    /**
     * SHA-256 文件哈希
     */
    private String fileHash;

    /**
     * RustFS / OSS 存储 URL
     */
    private String storageUrl;

    /**
     * 解析后的简历纯文本
     */
    private String resumeText;

    private AnalyzeStatus analyzeStatus;

    /**
     * 上传与解析必须完成的截止时间
     */
    private OffsetDateTime uploadDeadlineAt;

    /**
     * local_message 的逻辑引用，不建立跨模块外键
     */
    private Long cleanupMessageId;

    /**
     * 上传时间
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * [逻辑外键]→sys_users
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /**
     * 触发解析的调用链 ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    /**
     * 最后更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
