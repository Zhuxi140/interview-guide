package interview.anticheat.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.anticheat.mapper.AntiCheatLogMapper;
import interview.anticheat.model.entity.AntiCheatLog;
import interview.anticheat.model.req.AntiCheatEvidenceUploadTicketReq;
import interview.anticheat.model.req.AntiCheatLogSearchReq;
import interview.anticheat.model.vo.AntiCheatEvidencePresignVO;
import interview.anticheat.model.vo.AntiCheatEvidenceUploadTicketVO;
import interview.anticheat.model.vo.AntiCheatLogDetailVO;
import interview.anticheat.model.vo.AntiCheatLogListItemVO;
import interview.api.infra.FileStorageApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.FileSort;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.service.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * @author zhuxi
 * @apiNote 防作弊服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AntiCheatServiceImpl extends ServiceImpl<AntiCheatLogMapper, AntiCheatLog>
        implements AntiCheatService {

    private final FileStorageApi fileStorageApi;
    private final InterviewSessionService interviewSessionService;

    /** 上传/下载凭证有效期（秒） */
    private static final long TICKET_TTL_SECONDS = 300L;

    /** 证据白名单：抓拍截图与受限视频 */
    private static final Set<String> EVIDENCE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "video/mp4");

    @Override
    public AntiCheatEvidenceUploadTicketVO createEvidenceUploadTicket(
            Long sessionId, AntiCheatEvidenceUploadTicketReq req) {
        // 会话归属校验：复用同模块会话服务单表查询，仅候选人本人可为自己的会话上传证据。
        Long userId = AuthContext.getRequiredUserId();
        InterviewSession session = interviewSessionService.getById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND);
        }
        if (!userId.equals(session.getUserId())) {
            throw new BusinessException(ErrorCode.USER_NOT_PARTICIPANT);
        }

        // 证据媒体类型白名单；服务端上传后仍需重新检测媒体类型并做恶意文件扫描。
        if (!EVIDENCE_CONTENT_TYPES.contains(req.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "不支持的证据文件类型：仅接受 jpeg/png/webp 图片或 mp4 视频");
        }

        // 证据材料令牌 = RustFS 对象键：FileSort 暂无防作弊专用分类，使用 ATTACHMENT 兜底隔离。
        String materialToken = fileStorageApi.generateFileKey(
                req.getFileName(), FileSort.ATTACHMENT);

        // TODO [Phase8] FileStorageApi 暂无预签名 PUT 直传能力：直传网关接入后填充 uploadUrl。
        // TODO [Phase8] 批量上报（复杂接口）落地时：以 eventId 幂等绑定 sessionId+sha256+token，
        //  并由服务端写入 anti_cheat_logs（snapshot_oss_url = token）。
        return AntiCheatEvidenceUploadTicketVO.builder()
                .evidenceMaterialToken(materialToken)
                .uploadUrl(null)
                .expiresInSeconds(TICKET_TTL_SECONDS)
                .build();
    }

    @Override
    public IPage<AntiCheatLogListItemVO> pageLogs(AntiCheatLogSearchReq req) {
        // 单表 LambdaQuery：事件类型/用户/会话筛选，违规时间倒序。
        LambdaQueryWrapper<AntiCheatLog> wrapper = new LambdaQueryWrapper<AntiCheatLog>()
                .eq(req.getEventType() != null, AntiCheatLog::getEventType, req.getEventType())
                .eq(req.getUserId() != null, AntiCheatLog::getUserId, req.getUserId())
                .eq(req.getSessionId() != null, AntiCheatLog::getSessionId, req.getSessionId())
                .orderByDesc(AntiCheatLog::getCreatedAt)
                .orderByDesc(AntiCheatLog::getId);
        Page<AntiCheatLog> logPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        Page<AntiCheatLogListItemVO> voPage = new Page<>(
                logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        voPage.setRecords(logPage.getRecords().stream()
                .map(logEntry -> AntiCheatLogListItemVO.builder()
                        .id(logEntry.getId())
                        .userId(logEntry.getUserId())
                        .sessionId(logEntry.getSessionId())
                        .eventType(logEntry.getEventType())
                        .durationMs(logEntry.getDurationMs())
                        .evidenceAvailable(StrUtil.isNotBlank(logEntry.getSnapshotOssUrl()))
                        .createdAt(logEntry.getCreatedAt())
                        .build())
                .toList());
        return voPage;
    }

    @Override
    public AntiCheatLogDetailVO getLogDetail(Long logId) {
        // 单条详情；不返回证据对象键本身，仅返回是否可下载。
        AntiCheatLog logEntry = getRequiredLog(logId);
        return AntiCheatLogDetailVO.builder()
                .id(logEntry.getId())
                .userId(logEntry.getUserId())
                .sessionId(logEntry.getSessionId())
                .eventType(logEntry.getEventType())
                .durationMs(logEntry.getDurationMs())
                .evidenceAvailable(StrUtil.isNotBlank(logEntry.getSnapshotOssUrl()))
                .traceId(logEntry.getTraceId())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }

    @Override
    public AntiCheatEvidencePresignVO getEvidencePresign(Long logId) {
        // 仅对已关联证据的日志签发短期只读地址；敏感材料禁止落入普通日志。
        AntiCheatLog logEntry = getRequiredLog(logId);
        if (StrUtil.isBlank(logEntry.getSnapshotOssUrl())) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "该日志未关联证据材料");
        }
        return AntiCheatEvidencePresignVO.builder()
                .downloadUrl(fileStorageApi.generatePresignedDownloadUrl(
                        logEntry.getSnapshotOssUrl(), Duration.ofSeconds(TICKET_TTL_SECONDS)))
                .expiresInSeconds(TICKET_TTL_SECONDS)
                .build();
    }

    /**
     * 加载必须存在的防作弊日志
     * @param logId 日志 ID
     * @return 日志记录
     */
    private AntiCheatLog getRequiredLog(Long logId) {
        AntiCheatLog logEntry = lambdaQuery()
                .eq(AntiCheatLog::getId, logId)
                .one();
        if (logEntry == null) {
            // 100xxx 暂无防作弊日志不存在专用错误码，按既有先例使用参数校验错误并携带提示信息。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "防作弊日志不存在");
        }
        return logEntry;
    }
}
