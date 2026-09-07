package interview.api.system.dto;

import interview.common.enums.ChannelType;
import interview.common.enums.NotifyScene;

import java.util.Map;
import java.util.Set;

/**
 * 通知发送命令。跨模块调用方只传 ID 与基础值，禁止传递持久化实体。
 *
 * <p>接收人 {@code userId} 必须由调用方解析后显式传入，通知模块不负责
 * "谁是接收人" 的业务推导；{@code operatorUserId} 显式携带审计操作人，
 * 实现方不得依赖调用线程中的 {@code AuthContext}。</p>
 *
 * @param enterpriseId    企业租户 ID（候选人侧通知同样必填，取业务归属企业）
 * @param userId          接收人用户 ID
 * @param scene           业务场景，驱动文案与模板选择
 * @param channels        期望投递的渠道集合；站内信始终建议包含，外部渠道按渠道启用状态过滤
 * @param vars            场景文案变量；无变量场景允许传 null 或空集合
 * @param idempotencyKey  发送幂等键，建议格式 {scene}:{bizId}；允许为空，但业务重试路径必须提供
 * @param operatorUserId  审计操作人（触发本次通知的用户）；系统触发传 null
 */
public record SendNotificationCommand(
        Long enterpriseId,
        Long userId,
        NotifyScene scene,
        Set<ChannelType> channels,
        Map<String, String> vars,
        String idempotencyKey,
        Long operatorUserId
) {
}
