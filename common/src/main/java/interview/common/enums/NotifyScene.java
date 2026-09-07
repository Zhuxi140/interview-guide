package interview.common.enums;

/**
 * 通知业务场景，驱动模板选择。
 */
public enum NotifyScene {

    /**
     * 面试邀请。
     */
    INTERVIEW_INVITE,

    /**
     * 面试取消。
     */
    INTERVIEW_CANCEL,

    /**
     * Offer 已发送。
     */
    OFFER_SENT,

    /**
     * 候选人已决策 Offer。
     */
    OFFER_DECIDED,

    /**
     * 面评报告生成完毕。
     */
    REPORT_READY,

    /**
     * 系统通知。
     */
    SYSTEM
}
