package interview.billing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.billing.model.entity.WalletTransaction;
import interview.billing.model.enums.WalletTransactionType;
import interview.billing.model.vo.WalletTransactionListItemVO;

import java.time.OffsetDateTime;

public interface WalletTransactionService extends IService<WalletTransaction> {

    /**
     * 分页查询企业钱包权威账本
     * @param enterpriseId 企业 ID
     * @param page 当前页
     * @param size 每页数量
     * @param type 流水类型
     * @param startTime 创建时间起点
     * @param endTime 创建时间终点
     * @return 钱包流水分页
     */
    IPage<WalletTransactionListItemVO> pageTransactions(Long enterpriseId, Integer page, Integer size,
                                                         WalletTransactionType type,
                                                         OffsetDateTime startTime, OffsetDateTime endTime);
}
