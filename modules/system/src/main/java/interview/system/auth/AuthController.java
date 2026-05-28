package interview.system.auth;

import interview.common.constant.ApiVersion;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhuxi
 * @apiNote 认证控制器
 * @since 2026/5/27 14:05
 */

@RestController
@RequestMapping(ApiVersion.V1 + "/auth")
@Tag(name = "认证")
public class AuthController {
}
