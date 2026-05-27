package interview.system.auth;

import interview.common.constant.ApiVersion;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote 认证控制器
 * @since 2026/5/27 14:05
 */

@RestController
@RequestMapping(ApiVersion.V1 + "/auth")
public class AuthController {
}
