package interview.voiceinterview.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.Role;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.service.InterviewSessionService;
import interview.voiceinterview.mapper.VoiceInterviewMessageMapper;
import interview.voiceinterview.model.entity.VoiceInterviewMessage;
import interview.voiceinterview.model.vo.VoiceMessagePageVO;
import interview.voiceinterview.model.vo.VoiceMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoiceInterviewMessageServiceImpl
        extends ServiceImpl<VoiceInterviewMessageMapper, VoiceInterviewMessage>
        implements VoiceInterviewMessageService {

    private static final String VOICE_SESSION_TYPE = "VOICE";
    private static final long DEFAULT_AFTER_SEQUENCE = 0L;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final InterviewSessionService interviewSessionService;
    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    public VoiceMessagePageVO queryMessages(Long sessionId, Long afterSequence, Integer size) {
        // 纯 CRUD：校验查询参数和统一会话访问权限。
        long cursor = afterSequence == null
                ? DEFAULT_AFTER_SEQUENCE : afterSequence;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        validateQuery(cursor, pageSize);
        validateSessionAccess(sessionId);

        // 按事件序号游标多查询一条，用于判断是否还有下一页。
        List<VoiceInterviewMessage> messages = lambdaQuery()
                .select(
                        VoiceInterviewMessage::getId,
                        VoiceInterviewMessage::getEventId,
                        VoiceInterviewMessage::getMessageType,
                        VoiceInterviewMessage::getCurrentPhase,
                        VoiceInterviewMessage::getAsrText,
                        VoiceInterviewMessage::getLlmResponseText,
                        VoiceInterviewMessage::getSequenceNum,
                        VoiceInterviewMessage::getCreatedAt,
                        VoiceInterviewMessage::getTraceId
                )
                .eq(VoiceInterviewMessage::getInterviewSessionId, sessionId)
                .gt(VoiceInterviewMessage::getSequenceNum, cursor)
                .orderByAsc(VoiceInterviewMessage::getSequenceNum)
                .page(new Page<>(1, pageSize + 1L, false))
                .getRecords();

        // 截掉用于探测下一页的额外记录，并返回最后一条已展示记录的序号。
        boolean hasMore = messages.size() > pageSize;
        List<VoiceMessageVO> records = messages.stream()
                .limit(pageSize)
                .map(this::toVoiceMessageVO)
                .toList();
        long nextSequence = records.isEmpty()
                ? cursor : records.getLast().sequenceNum();
        return new VoiceMessagePageVO(nextSequence, hasMore, records);
    }

    private void validateQuery(long afterSequence, int size) {
        if (afterSequence < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private void validateSessionAccess(Long sessionId) {
        // 查询统一会话中的候选人和租户归属，非语音会话按不存在处理。
        InterviewSession session = interviewSessionService.lambdaQuery()
                .select(
                        InterviewSession::getId,
                        InterviewSession::getUserId,
                        InterviewSession::getEnterpriseId,
                        InterviewSession::getSessionType
                )
                .eq(InterviewSession::getId, sessionId)
                .one();
        if (session == null
                || !VOICE_SESSION_TYPE.equals(session.getSessionType())) {
            throw new BusinessException(ErrorCode.VOICE_SESSION_NOT_FOUND);
        }

        // 候选人仅能读取本人会话，企业成员必须再次校验租户归属。
        Long userId = AuthContext.getRequiredUserId();
        UserType userType = AuthContext.getUserType();
        if (userType == UserType.CANDIDATE) {
            if (!userId.equals(session.getUserId())) {
                throw new BusinessException(ErrorCode.VOICE_SESSION_NOT_FOUND);
            }
            return;
        }
        if (isSuperAdmin()) {
            return;
        }
        if (userType == UserType.ENTERPRISE_USER) {
            enterpriseValidationApi.validateEnterpriseBelong(
                    session.getEnterpriseId(), userId);
            return;
        }
        throw new BusinessException(ErrorCode.PERMISSION_DENIED);
    }

    private boolean isSuperAdmin() {
        List<Role> platformRoles =
                AuthContext.getRequiredAuthContext().platformRoleCodes();
        return platformRoles != null && platformRoles.contains(Role.SUPER_ADMIN);
    }

    private VoiceMessageVO toVoiceMessageVO(VoiceInterviewMessage message) {
        return new VoiceMessageVO(
                message.getId(),
                message.getEventId(),
                message.getMessageType(),
                message.getCurrentPhase(),
                message.getAsrText(),
                message.getLlmResponseText(),
                message.getSequenceNum(),
                message.getCreatedAt(),
                message.getTraceId()
        );
    }
}
