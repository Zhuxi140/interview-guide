package interview.system.tenant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.common.constant.SecureActionContext;
import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.req.EnterpriseBasicUpdateReq;
import interview.system.tenant.model.req.EnterpriseContactEmailUpdateReq;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.vo.EnterpriseContactEmailUpdateVO;
import interview.system.tenant.model.vo.EnterpriseContactPhoneUpdateVO;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import interview.system.tenant.model.vo.EnterpriseUpdateVO;
import interview.system.auth.model.vo.SecureChallengeStartVO;

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
     * 创建企业联系邮箱更新 Challenge
     * @param enterpriseId 企业 ID
     * @return Challenge 启动信息
     */
    SecureChallengeStartVO startContactEmailChallenge(Long enterpriseId);

    /**
     * 创建企业注销 Challenge
     * @param enterpriseId 企业 ID
     * @return Challenge 启动信息
     */
    SecureChallengeStartVO startDeletionChallenge(Long enterpriseId);

    /**
     * 更新企业联系邮箱
     * @param enterpriseId 企业 ID
     * @param secureActionContext 安全操作上下文
     * @param req 邮箱更新请求
     * @return 邮箱更新结果
     */
    EnterpriseContactEmailUpdateVO updateEnterpriseContactEmail(
            Long enterpriseId, SecureActionContext secureActionContext,
            EnterpriseContactEmailUpdateReq req);

    /**
     * 使用企业联系电话验证上下文更新联系电话
     * @param enterpriseId 企业 ID
     * @param secureActionContext 安全操作上下文
     * @return 联系电话更新结果
     */
    EnterpriseContactPhoneUpdateVO updateEnterpriseContactPhone(
            Long enterpriseId, SecureActionContext secureActionContext);

    /**
     * 注销企业（仅 OWNER）
     *
     * @param enterpriseId 企业 ID
     * @param secureActionContext 安全操作上下文
     */
    void deleteEnterprise(Long enterpriseId, SecureActionContext secureActionContext);
}
