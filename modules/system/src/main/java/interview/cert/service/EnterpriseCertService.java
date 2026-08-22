package interview.cert.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.cert.model.entity.SysEnterpriseCert;
import interview.cert.model.req.EnterpriseCertAuditReq;
import interview.cert.model.req.EnterpriseCertSearchReq;
import interview.cert.model.req.EnterpriseCertSubmitReq;
import interview.cert.model.req.EnterpriseCertUploadTicketReq;
import interview.cert.model.vo.EnterpriseCertAuditVO;
import interview.cert.model.vo.EnterpriseCertDetailVO;
import interview.cert.model.vo.EnterpriseCertLicensePresignVO;
import interview.cert.model.vo.EnterpriseCertListItemVO;
import interview.cert.model.vo.EnterpriseCertStatusVO;
import interview.cert.model.vo.EnterpriseCertSubmitVO;
import interview.cert.model.vo.EnterpriseCertUploadTicketVO;

/**
 * @author zhuxi
 * @apiNote 企业资质认证服务（B 端提交 + 平台端审核）
 */
public interface EnterpriseCertService extends IService<SysEnterpriseCert> {

    /**
     * 为当前企业签发营业执照短期上传凭证（校验企业归属与 OWNER/ADMIN 角色）
     * @param enterpriseId 企业租户 ID
     * @param req 上传凭证请求（文件元信息、SHA-256）
     * @return 材料令牌（对象键）与凭证有效期
     */
    EnterpriseCertUploadTicketVO createLicenseUploadTicket(Long enterpriseId,
                                                           EnterpriseCertUploadTicketReq req);

    /**
     * 提交企业资质认证：绑定执照材料并置为待审核
     * @param enterpriseId 企业租户 ID
     * @param req 认证材料（信用代码、法定代表人、执照材料令牌）
     * @return 提交结果
     */
    EnterpriseCertSubmitVO submitCertification(Long enterpriseId, EnterpriseCertSubmitReq req);

    /**
     * 查询企业资质认证审核状态
     * @param enterpriseId 企业租户 ID
     * @return 认证状态视图；无记录时返回 NOT_SUBMITTED 语义
     */
    EnterpriseCertStatusVO getCertificationStatus(Long enterpriseId);

    /**
     * 平台端原子审核企业资质（仅允许 待审核 → 通过/拒绝；同事务联动 enterprises.status）
     * @param certId 认证记录 ID
     * @param req 审核动作与拒绝原因
     * @return 审核结果
     */
    EnterpriseCertAuditVO auditCertification(Long certId, EnterpriseCertAuditReq req);

    /**
     * 平台端分页查询企业认证审核列表
     * @param req 分页与状态筛选条件
     * @return 审核列表分页
     */
    IPage<EnterpriseCertListItemVO> pageCertifications(EnterpriseCertSearchReq req);

    /**
     * 平台端查询企业认证审核详情（信用代码与法定代表人脱敏）
     * @param certId 认证记录 ID
     * @return 审核详情
     */
    EnterpriseCertDetailVO getCertificationDetail(Long certId);

    /**
     * 平台端获取营业执照短期审核地址
     * @param certId 认证记录 ID
     * @return 短期预签名下载地址与有效期
     */
    EnterpriseCertLicensePresignVO getLicensePresign(Long certId);
}
