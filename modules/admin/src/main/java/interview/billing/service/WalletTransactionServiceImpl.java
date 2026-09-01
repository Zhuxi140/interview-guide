package interview.billing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.billing.mapper.WalletTransactionMapper;
import interview.billing.model.entity.WalletTransaction;
import interview.billing.model.enums.WalletTransactionType;
import interview.billing.model.vo.WalletTransactionListItemVO;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl
        extends ServiceImpl<WalletTransactionMapper, WalletTransaction>
        implements WalletTransactionService {

    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    public IPage<WalletTransactionListItemVO> pageTransactions(Long enterpriseId, Integer page, Integer size,
                                                                WalletTransactionType type,
                                                                OffsetDateTime startTime, OffsetDateTime endTime) {
        // 校验企业成员身份以及分页、时间范围。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        validatePage(page, size);
        validateTimeRange(startTime, endTime);

        // 权威账本只读，查询始终限定企业并稳定倒序分页。
        IPage<WalletTransaction> transactionPage = lambdaQuery()
                .select(WalletTransaction::getId, WalletTransaction::getCommandId,
                        WalletTransaction::getBalanceChange, WalletTransaction::getFrozenChange,
                        WalletTransaction::getType, WalletTransaction::getReferenceType,
                        WalletTransaction::getReferenceId, WalletTransaction::getBalanceAfter,
                        WalletTransaction::getFrozenAfter, WalletTransaction::getCreatedAt,
                        WalletTransaction::getTraceId)
                .eq(WalletTransaction::getEnterpriseId, enterpriseId)
                .eq(type != null, WalletTransaction::getType, type)
                .ge(startTime != null, WalletTransaction::getCreatedAt, startTime)
                .le(endTime != null, WalletTransaction::getCreatedAt, endTime)
                .orderByDesc(WalletTransaction::getCreatedAt)
                .orderByDesc(WalletTransaction::getId)
                .page(new Page<>(page, size));

        // 映射余额变化、关联资源与变更后快照。
        return transactionPage.convert(transaction -> new WalletTransactionListItemVO(
                transaction.getId(),
                transaction.getCommandId(),
                transaction.getBalanceChange(),
                transaction.getFrozenChange(),
                transaction.getType(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getBalanceAfter(),
                transaction.getFrozenAfter(),
                transaction.getCreatedAt(),
                transaction.getTraceId()
        ));
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private void validateTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(ErrorCode.TIME_RANGE_INVALID);
        }
    }
}
