package interview.system.tenant;

import interview.common.constant.ApiVersion;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 企业内部团队成员表 前端控制器
 * </p>
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises/{enterpriseId}/members")
public class EnterpriseTeamMembersController {

}
