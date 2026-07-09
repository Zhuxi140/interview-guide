package interview.system.tenant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.common.constant.SecureActionContext;
import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.req.EnterpriseBasicUpdateReq;
import interview.system.tenant.model.req.EnterpriseContactUpdateReq;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import interview.system.tenant.model.vo.EnterpriseContactUpdateVO;
import interview.system.tenant.model.vo.EnterpriseUpdateVO;

import java.util.List;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) 服务类
 * </p>
 *
 * @author zhuxi
 */
public interface EnterprisesService extends IService<Enterprise> {

    /**
     * 创建企业
     * <p>当前用户自动成为企业 OWNER</p>
     *
     * @param req 创建企业请求
     * @return 创建结果
     */
    EnterpriseCreateBO createEnterprise(EnterpriseCreateReq req);

    /**
     * 查询当前用户所属企业列表
     *
     * @return 企业列表（含当前用户在企业的角色编码和成员数）
     */
    List<ListUserEnterprisesBO> listUserEnterprises();

    /**
     * 查询企业详情
     *
     * @param enterpriseId 企业 ID
     * @return 企业详情
     */
    EnterpriseDetailVO getEnterpriseDetail(Long enterpriseId);

    /**
     * 更新企业基本信息（名称、简称、行业、规模、Logo）
     *
     * @param enterpriseId 企业 ID
     * @param req          更新请求
     * @return 更新结果
     */
    EnterpriseUpdateVO updateEnterpriseBasic(Long enterpriseId, EnterpriseBasicUpdateReq req);

    /**
     * 更新企业联系方式（邮箱、手机）
     *
     * @param enterpriseId 企业 ID
     * @param req          更新请求
     * @return 更新结果
     */
    EnterpriseContactUpdateVO updateEnterpriseContact(Long enterpriseId, SecureActionContext secureActionContext, EnterpriseContactUpdateReq req);

    /**
     * 注销企业（仅 OWNER）
     *
     * @param enterpriseId 企业 ID
     */
    void deleteEnterprise(Long enterpriseId);
}
