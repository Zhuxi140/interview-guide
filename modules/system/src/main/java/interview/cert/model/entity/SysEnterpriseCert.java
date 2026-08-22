package interview.cert.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.cert.model.enums.CertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote B 端企业主体资质认证表（sys_enterprise_cert）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "sys_enterprise_cert", autoResultMap = true)
public class SysEnterpriseCert implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联企业租户 ID，未删除行上唯一 */
    private Long enterpriseId;

    /** 企业法定名称（提交时从 enterprises 快照） */
    private String companyName;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 法定代表人姓名 */
    private String legalPerson;

    /** 营业执照扫描件存储路径（由上传凭证签发的 materialToken 绑定） */
    private String licenseUrl;

    /** 审核状态：0 待审核 / 1 已通过 / 2 已拒绝 */
    private CertStatus auditStatus;

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 平台审核专员的 User ID */
    private Long auditorId;

    /** 资料提交时间 */
    private OffsetDateTime submitTime;

    /** 审核处理时间 */
    private OffsetDateTime auditTime;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
