package interview.system.tenant;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.system.tenant.mapper.EnterprisesMapper;
import interview.system.tenant.model.entity.Enterprise;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) 服务实现类
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
@Service
public class EnterprisesServiceImpl extends ServiceImpl<EnterprisesMapper, Enterprise> implements EnterprisesService {

}
