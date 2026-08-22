package interview.kyc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.kyc.model.entity.SysUserKyc;
import interview.kyc.model.enums.KycMaterialType;
import interview.kyc.model.req.KycAuditReq;
import interview.kyc.model.req.KycAuditSearchReq;
import interview.kyc.model.req.KycMaterialUploadTicketReq;
import interview.kyc.model.vo.KycAuditVO;
import interview.kyc.model.vo.KycDetailVO;
import interview.kyc.model.vo.KycListItemVO;
import interview.kyc.model.vo.KycMaterialPresignVO;
import interview.kyc.model.vo.KycMaterialUploadTicketVO;
import interview.kyc.model.vo.KycStatusVO;

/**
 * @author zhuxi
 * @apiNote 个人实名认证服务（C 端自查 + 平台端审核）
 */
public interface KycService extends IService<SysUserKyc> {

    /**
     * 为当前登录候选人签发 KYC 材料短期上传凭证
     * @param req 上传凭证请求（材料类型、文件元信息、SHA-256）
     * @return 材料令牌（对象键）与凭证有效期
     */
    KycMaterialUploadTicketVO createMaterialUploadTicket(KycMaterialUploadTicketReq req);

    /**
     * 查询当前登录候选人最新的实名认证审核状态
     * @return 认证状态视图；无记录时返回 NOT_SUBMITTED 语义
     */
    KycStatusVO getMyKycStatus();

    /**
     * 平台端原子审核个人实名（仅允许 待审核 → 通过/拒绝）
     * @param kycId 实名认证记录 ID
     * @param req 审核动作与拒绝原因
     * @return 审核结果
     */
    KycAuditVO auditKyc(Long kycId, KycAuditReq req);

    /**
     * 平台端分页查询实名认证审核列表
     * @param req 分页与状态筛选条件
     * @return 审核列表分页
     */
    IPage<KycListItemVO> pageKyc(KycAuditSearchReq req);

    /**
     * 平台端查询实名认证审核详情（姓名与证件号脱敏）
     * @param kycId 实名认证记录 ID
     * @return 审核详情
     */
    KycDetailVO getKycDetail(Long kycId);

    /**
     * 平台端获取指定 KYC 材料的短期审核地址
     * @param kycId 实名认证记录 ID
     * @param materialType 材料类型
     * @return 短期预签名下载地址与有效期
     */
    KycMaterialPresignVO getMaterialPresign(Long kycId, KycMaterialType materialType);
}
