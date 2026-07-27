package interview.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.billing.model.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {
}
