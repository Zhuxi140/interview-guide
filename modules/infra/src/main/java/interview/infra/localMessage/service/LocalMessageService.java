package interview.infra.localMessage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.infra.localMessage.model.entity.LocalMessage;
import interview.infra.localMessage.model.req.LocalMessagePageReq;

import java.time.OffsetDateTime;
import java.util.List;

public interface LocalMessageService extends IService<LocalMessage> {

    /**
     * 分页查询本地消息
     * @param req 分页和筛选参数
     * @return 消息分页
     */
    IPage<LocalMessage> pageQuery(LocalMessagePageReq req);

    /**
     * 获取消息详情
     * @param id 消息 ID
     * @return 消息详情
     */
    LocalMessage getDetail(Long id);

    /**
     * 人工重试失败消息
     * @param id 消息 ID
     * @return 重置后的消息
     */
    LocalMessage manualRetry(Long id);

    /**
     * 批量重试失败消息
     * @param ids 消息 ID 集合
     * @return 批量重试结果
     */
    BatchRetryResult batchRetry(List<Long> ids);

    /**
     * 原子领取到期消息
     * @param priority 消息优先级
     * @param workerId 工作节点 ID
     * @param leaseDurationSeconds 租约秒数
     * @param limit 批次数量
     * @return 已领取消息
     */
    List<LocalMessage> claimForDispatch(MsgPriority priority, String workerId,
                                        long leaseDurationSeconds, int limit);

    /**
     * 将仍由当前租约持有的消息标记成功
     * @param lease 带租约信息的消息
     * @return 是否更新成功
     */
    boolean markSuccessIfOwned(LocalMessage lease);

    /**
     * 将仍由当前租约持有的消息标记忽略
     * @param lease 带租约信息的消息
     * @param reason 忽略原因
     * @return 是否更新成功
     */
    boolean markIgnoredIfOwned(LocalMessage lease, String reason);

    /**
     * 更新仍由当前租约持有的消息重试状态
     * @param lease 带租约信息的消息
     * @param retryCount 重试次数
     * @param newStatus 新状态
     * @param nextRetry 下次重试时间
     * @param lastError 错误原因
     * @return 是否更新成功
     */
    boolean markRetryIfOwned(LocalMessage lease, int retryCount, MsgStatus newStatus,
                             OffsetDateTime nextRetry, String lastError);

    record BatchRetryResult(int successCount, int failCount, List<RetryItemResult> results) {
        public record RetryItemResult(Long id, boolean success, String error) {}
    }
}
