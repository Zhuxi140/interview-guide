package interview.billing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.billing.mapper.TokenConsumeLogMapper;
import interview.billing.model.entity.TokenConsumeLog;
import interview.billing.model.vo.TokenConsumeLogListItemVO;
import interview.common.enums.BizType;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TokenConsumeLogServiceImpl
        extends ServiceImpl<TokenConsumeLogMapper, TokenConsumeLog>
        implements TokenConsumeLogService {

    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    public IPage<TokenConsumeLogListItemVO> pageLogs(Long enterpriseId, Integer page, Integer size,
                                                      BizType bizType,
                                                      OffsetDateTime startTime, OffsetDateTime endTime) {
        // 校验企业成员身份以及分页、时间范围。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        validatePage(page, size);
        validateTimeRange(startTime, endTime);

        // 消耗明细始终按企业隔离，并使用稳定倒序分页。
        IPage<TokenConsumeLog> logPage = lambdaQuery()
                .select(TokenConsumeLog::getId, TokenConsumeLog::getWalletTransactionId,
                        TokenConsumeLog::getUserId, TokenConsumeLog::getBizType,
                        TokenConsumeLog::getBizId, TokenConsumeLog::getAttemptNo,
                        TokenConsumeLog::getTokensConsumed, TokenConsumeLog::getBalanceAfter,
                        TokenConsumeLog::getCreatedAt, TokenConsumeLog::getTraceId)
                .eq(TokenConsumeLog::getEnterpriseId, enterpriseId)
                .eq(bizType != null, TokenConsumeLog::getBizType, bizType)
                .ge(startTime != null, TokenConsumeLog::getCreatedAt, startTime)
                .le(endTime != null, TokenConsumeLog::getCreatedAt, endTime)
                .orderByDesc(TokenConsumeLog::getCreatedAt)
                .orderByDesc(TokenConsumeLog::getId)
                .page(new Page<>(page, size));

        // 明细仅用于审计展示，余额事实以钱包权威账本为准。
        return logPage.convert(log -> new TokenConsumeLogListItemVO(
                log.getId(),
                log.getWalletTransactionId(),
                log.getUserId(),
                log.getBizType(),
                log.getBizId(),
                log.getAttemptNo(),
                log.getTokensConsumed(),
                log.getBalanceAfter(),
                log.getCreatedAt(),
                log.getTraceId()
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
