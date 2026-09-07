package interview.api.system;

import interview.api.system.dto.SendNotificationCommand;

/**
 * 通知发送跨模块契约。
 *
 * <p>调用方视角只有"发起一次通知"一个动作：站内信同步落库，
 * 外部渠道（EMAIL / SMS）按渠道启用状态投递，现阶段为日志占位。
 * 方法加入调用方当前事务（默认 REQUIRED 传播），发送失败不回抛业务异常，
 * 不阻塞主流程；调用方不得依赖返回值判断业务成败。</p>
 *
 * <p>未来接入真实外部网关时，本契约签名不变，实现内部切换为
 * Outbox 异步投递，调用方零改动。</p>
 */
public interface NotificationApi {

    /**
     * 按场景向指定用户发送通知，同步完成站内信落库与外部渠道投递。
     *
     * @param command 发送命令（接收人、场景、渠道、文案变量、幂等键）
     * @return 站内信落库的通知 ID；未投递站内信渠道或发送失败时返回 null
     */
    Long send(SendNotificationCommand command);
}
