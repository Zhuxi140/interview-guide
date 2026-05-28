package interview.system.tenant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.tenant.model.entity.Enterprise;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) Mapper 接口
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
public interface EnterprisesMapper extends BaseMapper<Enterprise> {

}
