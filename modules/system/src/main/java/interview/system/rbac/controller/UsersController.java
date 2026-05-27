package interview.system.rbac.controller;

import interview.common.constant.ApiVersion;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 系统用户主表（核心用户表，多张表依赖它） 前端控制器
 * </p>
 *
 * @author zhuxi
 * @since 2026-05-27
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/users")
public class UsersController {

}
