package interview.common.enums;

/**
 * 通知发送渠道。
 */
public enum ChannelType {

    /**
     * 站内信，不依赖外部渠道配置，始终启用。
     */
    IN_APP,

    /**
     * 邮件渠道。
     */
    EMAIL,

    /**
     * 短信渠道。
     */
    SMS
}
