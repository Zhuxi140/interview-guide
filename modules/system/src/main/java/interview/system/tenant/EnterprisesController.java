package interview.system.tenant;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.vo.EnterpriseCreateVO;
import interview.system.tenant.model.vo.EnterpriseListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) 前端控制器
 * </p>
 *
 * @author zhuxi
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/enterprises")
@Tag(name = "企业管理")
@RequiredArgsConstructor
public class EnterprisesController {
    private final EnterprisesService enterprisesService;
    private final EnterpriseCovert covert;

    @ApiResponse(description = "创建企业（创建者自动成为 OWNER）")
    @PostMapping
    public Result<EnterpriseCreateVO> createEnterprise(EnterpriseCreateReq req) {
        EnterpriseCreateBO enterprise = enterprisesService.createEnterprise(req);
        EnterpriseCreateVO vo = covert.convertToEnterpriseCreateVO(enterprise);
        return Result.success(vo);
    }

    @ApiResponse(description = "查询当前用户所属企业列表")
    @GetMapping
    public Result<List<EnterpriseListItemVO>> getEnterprises() {
        List<ListUserEnterprisesBO> listUserEnterprisesBOS = enterprisesService.listUserEnterprises();
        List<EnterpriseListItemVO> vos = covert.BOCovertToEnterpriseListItemVO(listUserEnterprisesBOS);
        return Result.success(vos);
    }

}
