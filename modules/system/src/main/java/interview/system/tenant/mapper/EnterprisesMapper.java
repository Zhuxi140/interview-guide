package interview.system.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.tenant.model.entity.Enterprise;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) Mapper 接口
 * </p>
 *
 * @author zhuxi
 */
public interface EnterprisesMapper extends BaseMapper<Enterprise> {

}
