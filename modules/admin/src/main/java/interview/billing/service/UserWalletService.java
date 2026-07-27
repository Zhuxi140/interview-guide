package interview.billing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.billing.model.entity.UserWallet;
import interview.billing.model.vo.WalletVO;

public interface UserWalletService extends IService<UserWallet> {

    /**
     * 查询企业算力钱包
     * @param enterpriseId 企业 ID
     * @return 企业钱包信息
     */
    WalletVO getWallet(Long enterpriseId);
}
