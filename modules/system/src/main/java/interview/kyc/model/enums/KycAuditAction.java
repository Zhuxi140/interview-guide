package interview.kyc.model.enums;

/**
 * @author zhuxi
 * @apiNote 审核动作（通过/拒绝）
 */
public enum KycAuditAction {

    /**
     * 审核通过
     */
    APPROVE,

    /**
     * 审核拒绝（必须携带拒绝原因）
     */
    REJECT
}
