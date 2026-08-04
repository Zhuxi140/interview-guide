package interview.offer.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import interview.offer.model.enums.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 企业向候选人发出的录用邀请。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("offers")
public class Offer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long applicationId;

    private Long candidateUserId;

    private String title;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    @Builder.Default
    private String currency = "CNY";

    private LocalDate plannedStartDate;

    private String content;

    private OffsetDateTime expiresAt;

    @Builder.Default
    private OfferStatus status = OfferStatus.DRAFT;

    private String createIdempotencyKey;

    private String sendIdempotencyKey;

    private String decisionIdempotencyKey;

    private String decisionReason;

    private String withdrawReason;

    private OffsetDateTime sentAt;

    private OffsetDateTime decidedAt;

    private OffsetDateTime withdrawnAt;

    @Version
    private Integer version;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
