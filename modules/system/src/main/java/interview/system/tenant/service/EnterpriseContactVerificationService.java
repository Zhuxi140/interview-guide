package interview.system.tenant.service;

import interview.system.tenant.model.req.EnterpriseContactCodeVerifyReq;
import interview.system.tenant.model.req.EnterpriseContactNewPhoneReq;
import interview.system.tenant.model.vo.EnterpriseContactVerifyStartVO;

/**
 * 企业联系电话双重验证服务。
 *
 * @author zhuxi
 */
public interface EnterpriseContactVerificationService {

    /**
     * 启动企业联系电话验证流程
     * @param enterpriseId 企业 ID
     * @return 验证流程启动结果
     */
    EnterpriseContactVerifyStartVO start(Long enterpriseId);

    /**
     * 验证企业原联系电话
     * @param enterpriseId 企业 ID
     * @param req 验证码请求
     */
    void verifyOldPhone(Long enterpriseId, EnterpriseContactCodeVerifyReq req);

    /**
     * 发送企业新联系电话验证码
     * @param enterpriseId 企业 ID
     * @param req 新联系电话请求
     */
    void sendNewPhoneCode(Long enterpriseId, EnterpriseContactNewPhoneReq req);

    /**
     * 验证企业新联系电话并签发安全令牌
     * @param enterpriseId 企业 ID
     * @param req 验证码请求
     * @return 一次性安全操作令牌
     */
    String verifyNewPhone(Long enterpriseId, EnterpriseContactCodeVerifyReq req);

    /**
     * 完成并清理企业联系电话验证流程
     * @param flowId 验证流程 ID
     */
    void complete(String flowId);
}
