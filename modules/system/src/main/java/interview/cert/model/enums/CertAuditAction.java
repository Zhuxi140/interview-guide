package interview.cert.model.enums;

/**
 * @author zhuxi
 * @apiNote 企业资质审核动作（通过/拒绝）
 */
public enum CertAuditAction {

    /**
     * 审核通过（联动 enterprises.status → NORMAL）
     */
    APPROVE,

    /**
     * 审核拒绝（联动 enterprises.status → PENDING 待认证）
     */
    REJECT
}
