package interview.voiceinterview.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.Role;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.service.InterviewSessionService;
import interview.voiceinterview.mapper.VoiceInterviewEvaluationMapper;
import interview.voiceinterview.model.entity.VoiceInterviewEvaluation;
import interview.voiceinterview.model.enums.VoiceEvaluationStatus;
import interview.voiceinterview.model.vo.EvaluationDetailVO;
import interview.voiceinterview.model.vo.EvaluationVO;
import interview.voiceinterview.service.VoiceInterviewEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoiceInterviewEvaluationServiceImpl
        extends ServiceImpl<VoiceInterviewEvaluationMapper, VoiceInterviewEvaluation>
        implements VoiceInterviewEvaluationService {

    private static final String VOICE_SESSION_TYPE = "VOICE";

    private final InterviewSessionService interviewSessionService;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final ObjectMapper objectMapper;

    @Override
    public EvaluationVO getEvaluation(Long sessionId) {
        // 纯 CRUD：先校验统一会话类型和当前用户的资源访问权限。
        validateSessionAccess(sessionId);

        // 一场会话只读取一份评估任务；缺失记录不能伪装成 PENDING。
        VoiceInterviewEvaluation evaluation = lambdaQuery()
                .select(
                        VoiceInterviewEvaluation::getId,
                        VoiceInterviewEvaluation::getEvaluationStatus,
                        VoiceInterviewEvaluation::getAttemptNo,
                        VoiceInterviewEvaluation::getFailureReason,
                        VoiceInterviewEvaluation::getOverallScore,
                        VoiceInterviewEvaluation::getQuestionEvaluationsJson,
                        VoiceInterviewEvaluation::getStrengthsJson,
                        VoiceInterviewEvaluation::getImprovementsJson,
                        VoiceInterviewEvaluation::getCompletedAt
                )
                .eq(VoiceInterviewEvaluation::getInterviewSessionId, sessionId)
                .one();
        if (evaluation == null) {
            throw new BusinessException(ErrorCode.VOICE_EVALUATION_NOT_FOUND);
        }

        // 仅 COMPLETED 返回结果详情，仅 FAILED 暴露失败原因。
        VoiceEvaluationStatus status = evaluation.getEvaluationStatus();
        EvaluationDetailVO detail = status == VoiceEvaluationStatus.COMPLETED
                ? toEvaluationDetailVO(evaluation) : null;
        String failureReason = status == VoiceEvaluationStatus.FAILED
                ? evaluation.getFailureReason() : null;
        return new EvaluationVO(
                status,
                evaluation.getAttemptNo(),
                failureReason,
                detail
        );
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
        if (userType == UserType.HR) {
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

    private EvaluationDetailVO toEvaluationDetailVO(
            VoiceInterviewEvaluation evaluation) {
        // 将 JSONB 字符串还原为接口需要的数组结构。
        return new EvaluationDetailVO(
                evaluation.getId(),
                evaluation.getOverallScore(),
                parseObjectList(evaluation.getQuestionEvaluationsJson()),
                parseStringList(evaluation.getStrengthsJson()),
                parseStringList(evaluation.getImprovementsJson()),
                evaluation.getCompletedAt()
        );
    }

    private List<Object> parseObjectList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, Object[].class));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.JSON_TO_OBJECT_ERROR,
                    "语音面试逐题评估结果无法解析");
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.JSON_TO_OBJECT_ERROR,
                    "语音面试评估文本列表无法解析");
        }
    }
}
