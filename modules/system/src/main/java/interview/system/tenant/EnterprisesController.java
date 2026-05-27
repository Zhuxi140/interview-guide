package interview.system.tenant;

import interview.common.constant.ApiVersion;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) 前端控制器
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
@RestController
@RequestMapping(ApiVersion.V1 +"/enterprises")
public class EnterprisesController {

}
