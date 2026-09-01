package interview.cert.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.infra.FileStorageApi;
import interview.cert.mapper.SysEnterpriseCertMapper;
import interview.cert.model.entity.SysEnterpriseCert;
import interview.cert.model.enums.CertAuditAction;
import interview.cert.model.enums.CertStatus;
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
import interview.common.enums.ErrorCode;
import interview.common.enums.FileSort;
import interview.common.enums.Role;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.enums.EnterpriseStatus;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import interview.system.tenant.service.EnterprisesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * @author zhuxi
 * @apiNote 企业资质认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseCertServiceImpl extends ServiceImpl<SysEnterpriseCertMapper, SysEnterpriseCert>
        implements EnterpriseCertService {

    private final FileStorageApi fileStorageApi;
    private final EnterprisesService enterprisesService;
    private final EnterpriseTeamMembersService enterpriseTeamMembersService;

    /** 上传/下载凭证有效期（秒） */
    private static final long TICKET_TTL_SECONDS = 300L;

    /** 营业执照仅接受白名单图片与 PDF */
    private static final Set<String> LICENSE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    @Override
    public EnterpriseCertUploadTicketVO createLicenseUploadTicket(
            Long enterpriseId, EnterpriseCertUploadTicketReq req) {
        // 企业归属与 OWNER/ADMIN 角色校验。
        verifyEnterpriseAndManagerRole(enterpriseId);

        // 执照白名单：仅接受受限图片与 PDF；服务端上传后仍需重新检测媒体类型。
        if (!LICENSE_CONTENT_TYPES.contains(req.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "不支持的营业执照文件类型：仅接受 jpeg/png/webp 图片或 PDF");
        }

        // 材料令牌 = RustFS 对象键：按企业资质分类隔离存储路径。
        String materialToken = fileStorageApi.generateFileKey(
                req.getFileName(), FileSort.ENTERPRISE_CERT);

        // TODO [Phase8] FileStorageApi 暂无预签名 PUT 直传能力：直传网关接入后填充 uploadUrl。
        // TODO [Phase8] 材料令牌与企业的短期绑定（Redis）随直传通道接入，防止跨企业引用。
        return EnterpriseCertUploadTicketVO.builder()
                .materialToken(materialToken)
                .uploadUrl(null)
                .expiresInSeconds(TICKET_TTL_SECONDS)
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public EnterpriseCertSubmitVO submitCertification(
            Long enterpriseId, EnterpriseCertSubmitReq req) {
        // 企业归属与 OWNER/ADMIN 角色校验。
        verifyEnterpriseAndManagerRole(enterpriseId);

        // 企业名称由服务端读取 enterprises，客户端不得形成第二事实来源。
        Enterprise enterprise = enterprisesService.lambdaQuery()
                .select(Enterprise::getId, Enterprise::getName)
                .eq(Enterprise::getId, enterpriseId)
                .one();
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        // 执照材料令牌必须归属企业资质存储分类且对象真实存在，不接受任意外部 licenseUrl。
        if (!req.getLicenseMaterialToken().startsWith(FileSort.ENTERPRISE_CERT.name() + "/")
                || !fileStorageApi.fileExists(req.getLicenseMaterialToken())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "营业执照材料无效或尚未上传完成");
        }

        // TODO [Phase8] 第三方营业执照 OCR / 统一社会信用代码核验接入后，此处前置校验
        //  creditCode 与执照影像的一致性，未通过时直接拒绝提交。

        // 单企业单记录：PENDING/APPROVED 拒绝重复提交，REJECTED 允许覆盖重报。
        OffsetDateTime now = OffsetDateTime.now();
        SysEnterpriseCert existing = getByEnterpriseId(enterpriseId);
        if (existing == null) {
            SysEnterpriseCert cert = SysEnterpriseCert.builder()
                    .enterpriseId(enterpriseId)
                    .companyName(enterprise.getName())
                    .creditCode(req.getCreditCode())
                    .legalPerson(req.getLegalPerson())
                    .licenseUrl(req.getLicenseMaterialToken())
                    .auditStatus(CertStatus.PENDING)
                    .submitTime(now)
                    .build();
            save(cert);
            return toSubmitVO(cert);
        }
        if (existing.getAuditStatus() != CertStatus.REJECTED) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    existing.getAuditStatus() == CertStatus.PENDING
                            ? "企业认证审核中，请勿重复提交"
                            : "企业已通过资质认证，请勿重复提交");
        }

        // 拒绝后重报：条件更新（仍处 REJECTED）防并发覆盖，同时清空上一轮审核痕迹。
        boolean updated = lambdaUpdate()
                .eq(SysEnterpriseCert::getId, existing.getId())
                .eq(SysEnterpriseCert::getAuditStatus, CertStatus.REJECTED)
                .set(SysEnterpriseCert::getCompanyName, enterprise.getName())
                .set(SysEnterpriseCert::getCreditCode, req.getCreditCode())
                .set(SysEnterpriseCert::getLegalPerson, req.getLegalPerson())
                .set(SysEnterpriseCert::getLicenseUrl, req.getLicenseMaterialToken())
                .set(SysEnterpriseCert::getAuditStatus, CertStatus.PENDING)
                .set(SysEnterpriseCert::getRejectReason, null)
                .set(SysEnterpriseCert::getAuditorId, null)
                .set(SysEnterpriseCert::getAuditTime, null)
                .set(SysEnterpriseCert::getSubmitTime, now)
                .set(SysEnterpriseCert::getUpdatedAt, now)
                .set(SysEnterpriseCert::getTraceId, TraceUtil.getTraceId())
                .update();
        if (!updated) {
            throw submitConflict();
        }
        existing.setAuditStatus(CertStatus.PENDING);
        existing.setSubmitTime(now);
        return toSubmitVO(existing);
    }

    @Override
    public EnterpriseCertStatusVO getCertificationStatus(Long enterpriseId) {
        // 企业成员自查：仅校验归属，不要求 OWNER/ADMIN 角色。
        verifyEnterpriseMembership(enterpriseId);

        SysEnterpriseCert cert = getByEnterpriseId(enterpriseId);
        if (cert == null) {
            // 无记录：按接口契约返回 NOT_SUBMITTED 语义。
            return EnterpriseCertStatusVO.builder()
                    .auditStatus(CertStatus.NOT_SUBMITTED)
                    .build();
        }
        return EnterpriseCertStatusVO.builder()
                .id(cert.getId())
                .auditStatus(cert.getAuditStatus())
                .rejectReason(CertStatus.REJECTED == cert.getAuditStatus()
                        ? cert.getRejectReason() : null)
                .submitTime(cert.getSubmitTime())
                .auditTime(cert.getAuditTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public EnterpriseCertAuditVO auditCertification(Long certId, EnterpriseCertAuditReq req) {
        // 拒绝必填原因，通过不得携带原因。
        validateAuditAction(req);

        // 校验认证记录存在。
        SysEnterpriseCert existing = getRequiredCert(certId);

        // CAS：仅允许 待审核 → 通过/拒绝；失败视为并发审核冲突。
        CertStatus target = req.getAction() == CertAuditAction.APPROVE
                ? CertStatus.APPROVED : CertStatus.REJECTED;
        OffsetDateTime now = OffsetDateTime.now();
        boolean updated = lambdaUpdate()
                .eq(SysEnterpriseCert::getId, certId)
                .eq(SysEnterpriseCert::getAuditStatus, CertStatus.PENDING)
                .set(SysEnterpriseCert::getAuditStatus, target)
                .set(SysEnterpriseCert::getRejectReason,
                        req.getAction() == CertAuditAction.APPROVE ? null : req.getRejectReason())
                .set(SysEnterpriseCert::getAuditorId, AuthContext.getRequiredUserId())
                .set(SysEnterpriseCert::getAuditTime, now)
                .set(SysEnterpriseCert::getUpdatedAt, now)
                .set(SysEnterpriseCert::getTraceId, TraceUtil.getTraceId())
                .update();
        if (!updated) {
            throw auditConflict();
        }

        // 同事务联动企业状态：通过 → NORMAL；拒绝 → 保持待认证 PENDING。
        EnterpriseStatus linkedStatus = req.getAction() == CertAuditAction.APPROVE
                ? EnterpriseStatus.NORMAL : EnterpriseStatus.PENDING;
        boolean enterpriseUpdated = enterprisesService.lambdaUpdate()
                .eq(Enterprise::getId, existing.getEnterpriseId())
                .set(Enterprise::getStatus, linkedStatus)
                .set(Enterprise::getUpdatedAt, now)
                .set(Enterprise::getTraceId, TraceUtil.getTraceId())
                .update();
        if (!enterpriseUpdated) {
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }
        return EnterpriseCertAuditVO.builder()
                .id(certId)
                .auditStatus(target)
                .auditTime(now)
                .build();
    }

    @Override
    public IPage<EnterpriseCertListItemVO> pageCertifications(EnterpriseCertSearchReq req) {
        // 单表查询：企业名称取提交时快照列，无需关联 enterprises。
        LambdaQueryWrapper<SysEnterpriseCert> wrapper = new LambdaQueryWrapper<SysEnterpriseCert>()
                .select(SysEnterpriseCert::getId, SysEnterpriseCert::getEnterpriseId,
                        SysEnterpriseCert::getCompanyName, SysEnterpriseCert::getAuditStatus,
                        SysEnterpriseCert::getSubmitTime)
                .eq(req.getAuditStatus() != null,
                        SysEnterpriseCert::getAuditStatus, req.getAuditStatus())
                .orderByDesc(SysEnterpriseCert::getSubmitTime)
                .orderByDesc(SysEnterpriseCert::getId);
        Page<SysEnterpriseCert> certPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        Page<EnterpriseCertListItemVO> voPage = new Page<>(
                certPage.getCurrent(), certPage.getSize(), certPage.getTotal());
        voPage.setRecords(certPage.getRecords().stream()
                .map(cert -> EnterpriseCertListItemVO.builder()
                        .id(cert.getId())
                        .enterpriseId(cert.getEnterpriseId())
                        .companyName(cert.getCompanyName())
                        .auditStatus(cert.getAuditStatus())
                        .submitTime(cert.getSubmitTime())
                        .build())
                .toList());
        return voPage;
    }

    @Override
    public EnterpriseCertDetailVO getCertificationDetail(Long certId) {
        // 信用代码与法定代表人出参脱敏。
        SysEnterpriseCert cert = getRequiredCert(certId);
        return EnterpriseCertDetailVO.builder()
                .id(cert.getId())
                .enterpriseId(cert.getEnterpriseId())
                .companyName(cert.getCompanyName())
                .creditCodeMasked(maskCreditCode(cert.getCreditCode()))
                .legalPersonMasked(maskName(cert.getLegalPerson()))
                .auditStatus(cert.getAuditStatus())
                .rejectReason(CertStatus.REJECTED == cert.getAuditStatus()
                        ? cert.getRejectReason() : null)
                .submitTime(cert.getSubmitTime())
                .auditTime(cert.getAuditTime())
                .build();
    }

    @Override
    public EnterpriseCertLicensePresignVO getLicensePresign(Long certId) {
        // 校验记录存在后对存储对象键签发短期只读地址；敏感材料禁止落入普通日志。
        SysEnterpriseCert cert = getRequiredCert(certId);
        return EnterpriseCertLicensePresignVO.builder()
                .downloadUrl(fileStorageApi.generatePresignedDownloadUrl(
                        cert.getLicenseUrl(), Duration.ofSeconds(TICKET_TTL_SECONDS)))
                .expiresInSeconds(TICKET_TTL_SECONDS)
                .build();
    }

    /**
     * 校验企业存在且当前用户是其 OWNER 或 ADMIN
     * @param enterpriseId 企业租户 ID
     */
    private void verifyEnterpriseAndManagerRole(Long enterpriseId) {
        verifyEnterpriseMembership(enterpriseId);

        // 单表 LambdaQuery 校验企业内角色：仅 OWNER/ADMIN 可提交资质认证。
        Long userId = AuthContext.getRequiredUserId();
        boolean manager = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .in(EnterpriseTeamMember::getRoleId,
                        Role.ENTERPRISE_OWNER.getCode(), Role.ENTERPRISE_ADMIN.getCode())
                .exists();
        if (!manager) {
            throw new BusinessException(ErrorCode.NOT_ENTERPRISE_OWNER);
        }
    }

    /**
     * 校验企业存在且当前用户属于该企业（任意角色）
     * @param enterpriseId 企业租户 ID
     */
    private void verifyEnterpriseMembership(Long enterpriseId) {
        boolean enterpriseExists = enterprisesService.lambdaQuery()
                .eq(Enterprise::getId, enterpriseId)
                .exists();
        if (!enterpriseExists) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
        boolean member = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, AuthContext.getRequiredUserId())
                .exists();
        if (!member) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }
    }

    /**
     * 查询企业当前认证记录（未删除行上 enterprise_id 唯一）
     * @param enterpriseId 企业租户 ID
     * @return 认证记录；无记录时返回 null
     */
    private SysEnterpriseCert getByEnterpriseId(Long enterpriseId) {
        return lambdaQuery()
                .eq(SysEnterpriseCert::getEnterpriseId, enterpriseId)
                .one();
    }

    /**
     * 加载必须存在的认证记录
     * @param certId 记录 ID
     * @return 认证记录
     */
    private SysEnterpriseCert getRequiredCert(Long certId) {
        SysEnterpriseCert cert = lambdaQuery()
                .eq(SysEnterpriseCert::getId, certId)
                .one();
        if (cert == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CERT_NOT_FOUND);
        }
        return cert;
    }

    /**
     * 校验审核动作与拒绝原因的组合约束
     * @param req 审核请求
     */
    private void validateAuditAction(EnterpriseCertAuditReq req) {
        if (req.getAction() == CertAuditAction.REJECT && StrUtil.isBlank(req.getRejectReason())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "拒绝时必须填写拒绝原因");
        }
        if (req.getAction() == CertAuditAction.APPROVE && StrUtil.isNotBlank(req.getRejectReason())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "通过时不得携带拒绝原因");
        }
    }

    /**
     * 100xxx 暂无企业认证提交冲突专用错误码，按既有先例使用参数校验错误并携带提示信息
     * @return 提交冲突异常
     */
    private BusinessException submitConflict() {
        return new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                "企业认证已被其他请求处理，请刷新后重试");
    }

    /**
     * 100xxx 暂无企业认证审核并发专用错误码，按既有先例使用参数校验错误并携带提示信息
     * @return 审核冲突异常
     */
    private BusinessException auditConflict() {
        return new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                "企业认证已被其他请求审核，请刷新后重试");
    }

    /**
     * 组装提交结果
     * @param cert 认证记录
     * @return 提交结果 VO
     */
    private EnterpriseCertSubmitVO toSubmitVO(SysEnterpriseCert cert) {
        return EnterpriseCertSubmitVO.builder()
                .id(cert.getId())
                .enterpriseId(cert.getEnterpriseId())
                .auditStatus(cert.getAuditStatus())
                .submitTime(cert.getSubmitTime())
                .build();
    }

    /**
     * 统一社会信用代码脱敏：保留前 4 位与后 2 位
     * @param creditCode 信用代码明文
     * @return 脱敏信用代码
     */
    private String maskCreditCode(String creditCode) {
        if (StrUtil.isBlank(creditCode)) {
            return "";
        }
        if (creditCode.length() <= 6) {
            return "****";
        }
        return creditCode.substring(0, 4)
                + "*".repeat(creditCode.length() - 6)
                + creditCode.substring(creditCode.length() - 2);
    }

    /**
     * 姓名脱敏：保留首字符
     * @param name 姓名明文
     * @return 脱敏姓名
     */
    private String maskName(String name) {
        if (StrUtil.isBlank(name)) {
            return "";
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + "**";
    }
}
