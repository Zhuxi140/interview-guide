package interview.kyc.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.infra.FileStorageApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.FileSort;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.kyc.mapper.SysUserKycMapper;
import interview.kyc.model.entity.SysUserKyc;
import interview.kyc.model.enums.KycAuditAction;
import interview.kyc.model.enums.KycMaterialType;
import interview.kyc.model.enums.KycStatus;
import interview.kyc.model.req.KycAuditReq;
import interview.kyc.model.req.KycAuditSearchReq;
import interview.kyc.model.req.KycMaterialUploadTicketReq;
import interview.kyc.model.vo.KycAuditVO;
import interview.kyc.model.vo.KycDetailVO;
import interview.kyc.model.vo.KycListItemVO;
import interview.kyc.model.vo.KycMaterialPresignVO;
import interview.kyc.model.vo.KycMaterialUploadTicketVO;
import interview.kyc.model.vo.KycStatusVO;
import interview.kyc.service.KycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author zhuxi
 * @apiNote 个人实名认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycServiceImpl extends ServiceImpl<SysUserKycMapper, SysUserKyc>
        implements KycService {

    private final FileStorageApi fileStorageApi;

    /** 上传/下载凭证有效期（秒） */
    private static final long TICKET_TTL_SECONDS = 300L;

    /** 证件图片白名单；人脸活体材料按供应商协议额外接受受限视频 */
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");
    private static final Set<String> LIVENESS_EXTRA_CONTENT_TYPES = Set.of("video/mp4");

    @Override
    public KycMaterialUploadTicketVO createMaterialUploadTicket(KycMaterialUploadTicketReq req) {
        // 仅校验材料类型对应的 MIME 白名单；服务端上传后仍需重新检测媒体类型。
        boolean typeAllowed = IMAGE_CONTENT_TYPES.contains(req.getContentType())
                || (req.getMaterialType() == KycMaterialType.FACE_LIVENESS
                        && LIVENESS_EXTRA_CONTENT_TYPES.contains(req.getContentType()));
        if (!typeAllowed) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "不支持的实名材料类型：仅接受 jpeg/png/webp 图片"
                            + (req.getMaterialType() == KycMaterialType.FACE_LIVENESS
                                    ? " 或 mp4 视频" : ""));
        }

        // 材料令牌 = RustFS 对象键：按 KYC 证件分类隔离存储路径，不信任客户端文件名。
        String materialToken = fileStorageApi.generateFileKey(
                req.getFileName(), FileSort.KYC_ID_CARD);

        // TODO [Phase8] FileStorageApi 暂无预签名 PUT 直传能力：直传网关接入后填充 uploadUrl，
        //  当前客户端需携带 materialToken 经服务端中转上传。
        // TODO [Phase8] 第三方实名提交接口落地时：将 userId+materialType+sha256 与 token 绑定
        //  并保证单次消费，防止跨用户引用材料。
        return KycMaterialUploadTicketVO.builder()
                .materialToken(materialToken)
                .uploadUrl(null)
                .expiresInSeconds(TICKET_TTL_SECONDS)
                .build();
    }

    @Override
    public KycStatusVO getMyKycStatus() {
        // user_id 在未删除行上唯一，单条查询即本人最新记录。
        SysUserKyc kyc = lambdaQuery()
                .eq(SysUserKyc::getUserId, AuthContext.getRequiredUserId())
                .one();
        if (kyc == null) {
            // 无记录：按接口契约返回 NOT_SUBMITTED 语义，不落库、不报错。
            return KycStatusVO.builder()
                    .authStatus(KycStatus.NOT_SUBMITTED)
                    .build();
        }
        return KycStatusVO.builder()
                .id(kyc.getId())
                .authStatus(kyc.getAuthStatus())
                .rejectReason(KycStatus.REJECTED == kyc.getAuthStatus()
                        ? kyc.getRejectReason() : null)
                .submitTime(kyc.getSubmitTime())
                .auditTime(kyc.getAuditTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public KycAuditVO auditKyc(Long kycId, KycAuditReq req) {
        // 拒绝必填原因，通过不得携带原因，防止审核语义被污染。
        validateAuditAction(req);

        // 校验记录存在。
        SysUserKyc existing = getRequiredKyc(kycId);

        // CAS：仅允许 待审核 → 通过/拒绝；条件更新失败视为并发审核冲突。
        KycStatus target = req.getAction() == KycAuditAction.APPROVE
                ? KycStatus.APPROVED : KycStatus.REJECTED;
        OffsetDateTime now = OffsetDateTime.now();
        boolean updated = lambdaUpdate()
                .eq(SysUserKyc::getId, kycId)
                .eq(SysUserKyc::getAuthStatus, KycStatus.PENDING)
                .set(SysUserKyc::getAuthStatus, target)
                .set(SysUserKyc::getRejectReason,
                        req.getAction() == KycAuditAction.APPROVE ? null : req.getRejectReason())
                .set(SysUserKyc::getAuditTime, now)
                .set(SysUserKyc::getUpdatedAt, now)
                .set(SysUserKyc::getTraceId, TraceUtil.getTraceId())
                .update();
        if (!updated) {
            throw auditConflict();
        }
        return KycAuditVO.builder()
                .id(kycId)
                .authStatus(target)
                .auditTime(now)
                .build();
    }

    @Override
    public IPage<KycListItemVO> pageKyc(KycAuditSearchReq req) {
        // NOT_SUBMITTED 仅为返回语义，作为筛选项时结果恒为空。
        LambdaQueryWrapper<SysUserKyc> wrapper = new LambdaQueryWrapper<SysUserKyc>()
                .select(SysUserKyc::getId, SysUserKyc::getUserId, SysUserKyc::getRealName,
                        SysUserKyc::getAuthStatus, SysUserKyc::getSubmitTime)
                .eq(req.getAuthStatus() != null, SysUserKyc::getAuthStatus, req.getAuthStatus())
                .orderByDesc(SysUserKyc::getSubmitTime)
                .orderByDesc(SysUserKyc::getId);
        Page<SysUserKyc> kycPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        Page<KycListItemVO> voPage = new Page<>(
                kycPage.getCurrent(), kycPage.getSize(), kycPage.getTotal());
        voPage.setRecords(kycPage.getRecords().stream()
                .map(kyc -> KycListItemVO.builder()
                        .id(kyc.getId())
                        .userId(kyc.getUserId())
                        .maskedRealName(maskName(kyc.getRealName()))
                        .authStatus(kyc.getAuthStatus())
                        .submitTime(kyc.getSubmitTime())
                        .build())
                .toList());
        return voPage;
    }

    @Override
    public KycDetailVO getKycDetail(Long kycId) {
        // 敏感字段（姓名/证件号）出参一律脱敏，明文不得离开服务端。
        SysUserKyc kyc = getRequiredKyc(kycId);
        return KycDetailVO.builder()
                .id(kyc.getId())
                .userId(kyc.getUserId())
                .maskedRealName(maskName(kyc.getRealName()))
                .maskedIdCardNo(maskIdCardNo(kyc.getIdCardNo()))
                .authStatus(kyc.getAuthStatus())
                .rejectReason(KycStatus.REJECTED == kyc.getAuthStatus()
                        ? kyc.getRejectReason() : null)
                .materialTypes(STANDARD_MATERIAL_TYPES)
                .submitTime(kyc.getSubmitTime())
                .auditTime(kyc.getAuditTime())
                .build();
    }

    @Override
    public KycMaterialPresignVO getMaterialPresign(Long kycId, KycMaterialType materialType) {
        // 校验实名记录存在后签发短期只读地址；敏感材料禁止落入普通日志。
        getRequiredKyc(kycId);

        // TODO [Phase8] 实名提交（复杂接口）落地材料绑定表后，应从绑定记录读取真实对象键；
        //  当前按 kycId+materialType 约定键签发，对象在提交接口上传后即可访问。
        String materialKey = String.format("%s/%d/%s",
                FileSort.KYC_ID_CARD.name(), kycId, materialType.name());
        return KycMaterialPresignVO.builder()
                .downloadUrl(fileStorageApi.generatePresignedDownloadUrl(
                        materialKey, Duration.ofSeconds(TICKET_TTL_SECONDS)))
                .expiresInSeconds(TICKET_TTL_SECONDS)
                .build();
    }

    /** 标准实名材料集合（提交接口落地后由绑定记录替换） */
    private static final List<KycMaterialType> STANDARD_MATERIAL_TYPES =
            List.of(KycMaterialType.ID_CARD_FRONT,
                    KycMaterialType.ID_CARD_BACK, KycMaterialType.FACE_LIVENESS);

    /**
     * 校验审核动作与拒绝原因的组合约束
     * @param req 审核请求
     */
    private void validateAuditAction(KycAuditReq req) {
        if (req.getAction() == KycAuditAction.REJECT && StrUtil.isBlank(req.getRejectReason())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "拒绝时必须填写拒绝原因");
        }
        if (req.getAction() == KycAuditAction.APPROVE && StrUtil.isNotBlank(req.getRejectReason())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "通过时不得携带拒绝原因");
        }
    }

    /**
     * 加载必须存在的实名认证记录
     * @param kycId 记录 ID
     * @return 实名认证记录
     */
    private SysUserKyc getRequiredKyc(Long kycId) {
        SysUserKyc kyc = lambdaQuery()
                .eq(SysUserKyc::getId, kycId)
                .one();
        if (kyc == null) {
            throw new BusinessException(ErrorCode.KYC_NOT_FOUND);
        }
        return kyc;
    }

    /**
     * 100xxx 暂无审核并发专用错误码，按既有先例使用参数校验错误并携带提示信息
     * @return 并发冲突异常
     */
    private BusinessException auditConflict() {
        return new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                "实名认证已被其他请求审核，请刷新后重试");
    }

    /**
     * 姓名脱敏：保留首字符
     * @param realName 真实姓名明文
     * @return 脱敏姓名
     */
    private String maskName(String realName) {
        if (StrUtil.isBlank(realName)) {
            return "";
        }
        if (realName.length() == 1) {
            return realName;
        }
        return realName.charAt(0) + "**";
    }

    /**
     * 身份证号脱敏：保留前 4 位与后 4 位
     * @param idCardNo 身份证号明文
     * @return 脱敏身份证号
     */
    private String maskIdCardNo(String idCardNo) {
        if (StrUtil.isBlank(idCardNo)) {
            return "";
        }
        if (idCardNo.length() <= 8) {
            return "****";
        }
        return idCardNo.substring(0, 4)
                + "*".repeat(idCardNo.length() - 8)
                + idCardNo.substring(idCardNo.length() - 4);
    }
}
