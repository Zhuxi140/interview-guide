package interview.system.tenant;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) 服务类
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
public interface EnterprisesService extends IService<Enterprise> {

}
