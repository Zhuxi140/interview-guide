package interview.tutor.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewReportGenerationStatus;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.mapper.InterviewReportMapper;
import interview.textinterview.model.entity.InterviewReport;
import interview.tutor.mapper.AiTutorMessageMapper;
import interview.tutor.mapper.AiTutorSessionMapper;
import interview.tutor.model.entity.AiTutorMessage;
import interview.tutor.model.entity.AiTutorSession;
import interview.tutor.model.enums.TutorMessageRole;
import interview.tutor.model.req.TutorSessionCreateReq;
import interview.tutor.model.vo.TutorMessageListItemVO;
import interview.tutor.model.vo.TutorMessagePageVO;
import interview.tutor.model.vo.TutorSessionCreateVO;
import interview.tutor.model.vo.TutorSessionListItemVO;
import interview.tutor.service.AiTutorSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * C 端 AI 答疑会话服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class AiTutorSessionServiceImpl
        extends ServiceImpl<AiTutorSessionMapper, AiTutorSession>
        implements AiTutorSessionService {

    private static final String DEFAULT_SESSION_TITLE = "面评报告答疑";
    private static final int MAX_MESSAGE_PAGE_SIZE = 100;

    private final AiTutorMessageMapper aiTutorMessageMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final InterviewScheduleQueryApi interviewScheduleQueryApi;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public TutorSessionCreateVO createSession(TutorSessionCreateReq req) {
        // 查询关联报告并校验存在且未删除。
        Long userId = AuthContext.getRequiredUserId();
        InterviewReport report = interviewReportMapper.selectById(req.getAssociatedReportId());
        if (report == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_REPORT_NOT_FOUND);
        }

        // 报告归属校验：经排期模块确认报告对应排期属于当前候选人。
        InterviewScheduleQueryDTO schedule =
                interviewScheduleQueryApi.getSchedule(report.getScheduleId());
        if (schedule == null || !userId.equals(schedule.candidateUserId())) {
            throw new BusinessException(ErrorCode.INTERVIEW_REPORT_NOT_FOUND);
        }

        // 仅已完成生成的报告可发起答疑。
        if (report.getGenerationStatus() != InterviewReportGenerationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "仅已完成生成的面评报告可发起答疑");
        }

        // 创建会话：未提交标题时使用默认标题，企业域沿用报告所属企业。
        String sessionTitle = req.getSessionTitle() == null || req.getSessionTitle().isBlank()
                ? DEFAULT_SESSION_TITLE
                : req.getSessionTitle().trim();
        AiTutorSession session = AiTutorSession.builder()
                .enterpriseId(schedule.enterpriseId())
                .userId(userId)
                .associatedReportId(req.getAssociatedReportId())
                .sessionTitle(sessionTitle)
                .build();
        save(session);
        // TODO: 异步 AI 答疑消息发送接入后（Idempotency-Key + 异步任务），在此触发首条引导消息。
        return new TutorSessionCreateVO(
                session.getId(),
                session.getSessionTitle(),
                session.getAssociatedReportId(),
                session.getCreatedAt());
    }

    @Override
    public IPage<TutorSessionListItemVO> pageMySessions(Integer page, Integer size) {
        // 校验分页参数。
        validatePage(page, size);

        // 单表 LambdaQuery：仅查询本人会话，按创建时间倒序稳定分页。
        IPage<AiTutorSession> sessionPage = lambdaQuery()
                .select(AiTutorSession::getId, AiTutorSession::getSessionTitle,
                        AiTutorSession::getAssociatedReportId, AiTutorSession::getCreatedAt)
                .eq(AiTutorSession::getUserId, AuthContext.getRequiredUserId())
                .orderByDesc(AiTutorSession::getCreatedAt)
                .orderByDesc(AiTutorSession::getId)
                .page(new Page<>(page, size));
        if (sessionPage.getRecords().isEmpty()) {
            return new Page<>(sessionPage.getCurrent(), sessionPage.getSize(),
                    sessionPage.getTotal());
        }

        // 单表 IN 批量统计各会话消息数，Service 层按 sessionId 合并，避免 N+1。
        List<Long> sessionIds = sessionPage.getRecords().stream()
                .map(AiTutorSession::getId)
                .toList();
        Map<Long, Long> messageCountMap = aiTutorMessageMapper.selectList(
                        Wrappers.<AiTutorMessage>lambdaQuery()
                                .select(AiTutorMessage::getSessionId)
                                .in(AiTutorMessage::getSessionId, sessionIds))
                .stream()
                .collect(Collectors.groupingBy(
                        AiTutorMessage::getSessionId, Collectors.counting()));

        // 组装列表 VO。
        List<TutorSessionListItemVO> records = sessionPage.getRecords().stream()
                .map(session -> new TutorSessionListItemVO(
                        session.getId(),
                        session.getSessionTitle(),
                        session.getAssociatedReportId(),
                        messageCountMap.getOrDefault(session.getId(), 0L),
                        session.getCreatedAt()))
                .toList();
        Page<TutorSessionListItemVO> voPage = new Page<>(
                sessionPage.getCurrent(), sessionPage.getSize(), sessionPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public TutorMessagePageVO listMessages(Long sessionId, Long cursor, Integer size) {
        // 校验会话归属当前用户。
        requireMySession(sessionId);
        Long effectiveCursor = cursor == null ? 0L : cursor;
        int effectiveSize = size == null ? 50 : size;
        if (effectiveCursor < 0 || effectiveSize < 1 || effectiveSize > MAX_MESSAGE_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }

        // 多取一条判断 hasMore，避免偏移分页在消息增长后产生重复或遗漏。
        List<AiTutorMessage> rows = aiTutorMessageMapper.selectPage(
                        new Page<>(1, effectiveSize + 1L, false),
                        Wrappers.<AiTutorMessage>lambdaQuery()
                                .select(AiTutorMessage::getId, AiTutorMessage::getMessageType,
                                        AiTutorMessage::getContent, AiTutorMessage::getCreatedAt)
                                .eq(AiTutorMessage::getSessionId, sessionId)
                                .gt(AiTutorMessage::getId, effectiveCursor)
                                .orderByAsc(AiTutorMessage::getId))
                .getRecords();
        boolean hasMore = rows.size() > effectiveSize;
        List<TutorMessageListItemVO> records = rows.stream()
                .limit(effectiveSize)
                .map(message -> new TutorMessageListItemVO(
                        message.getId(),
                        TutorMessageRole.fromMessageType(message.getMessageType()),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
        Long nextCursor = hasMore && !records.isEmpty()
                ? records.getLast().id()
                : null;
        return new TutorMessagePageVO(nextCursor, hasMore, records);
    }

    /**
     * 校验会话存在且属于当前用户，否则按不存在处理。
     */
    private AiTutorSession requireMySession(Long sessionId) {
        AiTutorSession session = lambdaQuery()
                .select(AiTutorSession::getId, AiTutorSession::getUserId)
                .eq(AiTutorSession::getId, sessionId)
                .one();
        if (session == null || !Objects.equals(
                AuthContext.getRequiredUserId(), session.getUserId())) {
            // TODO: ErrorCode 缺少 TUTOR_SESSION_NOT_FOUND，暂以参数错误语义返回。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "答疑会话不存在");
        }
        return session;
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }
}
