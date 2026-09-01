package interview.billing.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.billing.mapper.UserWalletMapper;
import interview.billing.model.entity.UserWallet;
import interview.billing.model.vo.WalletVO;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserWalletServiceImpl
        extends ServiceImpl<UserWalletMapper, UserWallet>
        implements UserWalletService {

    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    public WalletVO getWallet(Long enterpriseId) {
        // 钱包按企业归属，先校验当前用户的企业成员身份。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        UserWallet wallet = lambdaQuery()
                .eq(UserWallet::getEnterpriseId, enterpriseId)
                .one();
        if (wallet == null) {
            throw new BusinessException(ErrorCode.WALLET_NOT_FOUND);
        }

        // 返回真实持久化钱包，禁止构造无记录的零余额对象。
        return new WalletVO(
                wallet.getId(),
                wallet.getEnterpriseId(),
                wallet.getBalance(),
                wallet.getFrozenBalance(),
                wallet.getTotalRecharged(),
                wallet.getVersion(),
                wallet.getUpdatedAt()
        );
    }
}
