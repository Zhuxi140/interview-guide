package interview.candidate.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.candidate.model.enums.EnterpriseCandidateSource;
import interview.candidate.model.enums.EnterpriseCandidateStatus;
import interview.framework.mybatis.JsonbStringTypeHandler;
import interview.framework.security.mybatis.AesTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "enterprise_candidates", autoResultMap = true)
public class EnterpriseCandidate implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private Long linkedUserId;
    private String candidateName;

    @TableField(value = "phone_ciphertext", typeHandler = AesTypeHandler.class)
    private String phone;

    @TableField(value = "email_ciphertext", typeHandler = AesTypeHandler.class)
    private String email;

    private EnterpriseCandidateSource source;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String profileJson;

    private EnterpriseCandidateStatus status;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
