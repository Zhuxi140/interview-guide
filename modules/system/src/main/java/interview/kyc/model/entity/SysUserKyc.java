package interview.kyc.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.framework.security.mybatis.AesTypeHandler;
import interview.kyc.model.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote C 端个人实名认证表（sys_user_kyc）；real_name / id_card_no 密文存储
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "sys_user_kyc", autoResultMap = true)
public class SysUserKyc implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的 C 端求职者用户 ID，未删除行上唯一 */
    private Long userId;

    /** 真实姓名（AES 密文存储） */
    @TableField(typeHandler = AesTypeHandler.class)
    private String realName;

    /** 身份证号（AES 密文存储） */
    @TableField(typeHandler = AesTypeHandler.class)
    private String idCardNo;

    /** 活体人脸核验状态 */
    private Boolean faceVerified;

    /** 综合认证状态：0 待审核 / 1 已通过 / 2 已拒绝 */
    private KycStatus authStatus;

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 首次提交时间 */
    private OffsetDateTime submitTime;

    /** 最终审核处理时间 */
    private OffsetDateTime auditTime;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
