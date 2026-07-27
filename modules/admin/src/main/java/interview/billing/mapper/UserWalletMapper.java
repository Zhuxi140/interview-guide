package interview.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.billing.model.entity.UserWallet;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserWalletMapper extends BaseMapper<UserWallet> {
}
