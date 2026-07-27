package interview.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.billing.model.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
