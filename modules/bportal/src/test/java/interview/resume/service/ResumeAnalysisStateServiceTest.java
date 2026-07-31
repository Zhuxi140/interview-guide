package interview.resume.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.enums.ErrorCode;
import interview.common.enums.MessageHandleStatus;
import interview.common.enums.MsgTopic;
import interview.common.exception.BusinessException;
import interview.common.spi.MessageHandleResult;
import interview.resume.event.ResumeAnalysisCreatedEvent;
import interview.resume.mapper.ResumeAnalysesMapper;
import interview.resume.mapper.ResumesMapper;
import interview.resume.model.bo.ResumeAnalysisExecutionBO;
import interview.resume.model.entity.ResumeAnalyses;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.vo.ResumeAnalyzeTriggerVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisStateServiceTest {

    @Mock
    private ResumesMapper resumesMapper;
    @Mock
    private ResumeAnalysesMapper resumeAnalysesMapper;
    @Mock
    private LocalMessageApi localMessageApi;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Resumes.class);
    }

    @Test
    void accept_shouldCreateDelayedFallbackMessageAndReturnItsId() {
        ResumeAnalysisStateService service = service();
        when(resumesMapper.selectOne(any())).thenReturn(readyResume(10L));
        when(localMessageApi.findIdByBizKey(
                any(MsgTopic.class), any())).thenReturn(null);
        when(localMessageApi.saveInCurrentTransaction(any())).thenReturn(99L);
        when(resumesMapper.bindAnalysisRequest(
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResumeAnalyzeTriggerVO result = service.accept(1L, 10L, "idem-1");

        assertEquals(99L, result.taskId());
        assertEquals(AnalyzeStatus.PENDING, result.analyzeStatus());
        ArgumentCaptor<MessageDTO> messageCaptor =
                ArgumentCaptor.forClass(MessageDTO.class);
        verify(localMessageApi).saveInCurrentTransaction(messageCaptor.capture());
        assertEquals(MsgTopic.RESUME_AI_PARSER,
                messageCaptor.getValue().getTopic());
        verify(eventPublisher).publishEvent(
                new ResumeAnalysisCreatedEvent(99L, 10L, 1L));
    }

    @Test
    void accept_shouldReplayCurrentRequestWithoutCreatingMessage() {
        String keyHash = DigestUtil.sha256Hex("1:10:idem-1");
        Resumes resume = readyResume(10L);
        resume.setAnalyzeStatus(AnalyzeStatus.PROCESSING);
        resume.setAnalysisMessageId(20L);
        resume.setAnalysisIdempotencyKeyHash(keyHash);
        when(resumesMapper.selectOne(any())).thenReturn(resume);

        ResumeAnalyzeTriggerVO result =
                service().accept(1L, 10L, "idem-1");

        assertEquals(20L, result.taskId());
        assertEquals(AnalyzeStatus.PROCESSING, result.analyzeStatus());
        verify(localMessageApi, never()).saveInCurrentTransaction(any());
    }

    @Test
    void accept_shouldAllowSameKeyForDifferentResumes() {
        when(resumesMapper.selectOne(any()))
                .thenReturn(readyResume(10L), readyResume(11L));
        when(localMessageApi.findIdByBizKey(
                any(MsgTopic.class), any())).thenReturn(null);
        when(localMessageApi.saveInCurrentTransaction(any()))
                .thenReturn(20L, 21L);
        when(resumesMapper.bindAnalysisRequest(
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        ResumeAnalysisStateService service = service();
        ResumeAnalyzeTriggerVO first = service.accept(1L, 10L, "same-key");
        ResumeAnalyzeTriggerVO second = service.accept(1L, 11L, "same-key");

        assertNotEquals(first.taskId(), second.taskId());
        ArgumentCaptor<MessageDTO> messages =
                ArgumentCaptor.forClass(MessageDTO.class);
        verify(localMessageApi, times(2))
                .saveInCurrentTransaction(messages.capture());
        assertNotEquals(messages.getAllValues().get(0).getBizKey(),
                messages.getAllValues().get(1).getBizKey());
    }

    @Test
    void accept_shouldRejectNewKeyWhileAnalysisIsActive() {
        Resumes resume = readyResume(10L);
        resume.setAnalyzeStatus(AnalyzeStatus.PROCESSING);
        resume.setAnalysisMessageId(20L);
        resume.setAnalysisIdempotencyKeyHash("another-hash");
        when(resumesMapper.selectOne(any())).thenReturn(resume);
        when(localMessageApi.findIdByBizKey(
                any(MsgTopic.class), any())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service().accept(1L, 10L, "new-key"));

        assertEquals(ErrorCode.RESUME_ANALYZE_STATUS_ERROR.getCode(),
                exception.getCode());
        verify(localMessageApi, never()).saveInCurrentTransaction(any());
    }

    @Test
    void complete_shouldUseMessageIdAsResultId() {
        ResumeAnalysisExecutionBO execution =
                new ResumeAnalysisExecutionBO(
                        20L, 10L, 1L, "resume text", 1);
        when(resumesMapper.completeAnalysis(
                any(), any(), any(), anyInt(), any(), any())).thenReturn(1);

        boolean completed = service().complete(
                execution, 80, "[\"strength\"]",
                "[\"suggestion\"]", "{\"schemaVersion\":1}");

        assertEquals(true, completed);
        ArgumentCaptor<ResumeAnalyses> analysis =
                ArgumentCaptor.forClass(ResumeAnalyses.class);
        verify(resumeAnalysesMapper).insert(analysis.capture());
        assertEquals(20L, analysis.getValue().getId());
        assertEquals(10L, analysis.getValue().getResumeId());
    }

    @Test
    void fail_shouldRetryOnceThenBecomePermanent() {
        ResumeAnalysisExecutionBO firstAttempt =
                new ResumeAnalysisExecutionBO(
                        20L, 10L, 1L, "resume text", 1);
        when(resumesMapper.releaseAnalysisForRetry(
                any(), any(), any(), anyInt(), any(), any())).thenReturn(1);

        MessageHandleResult retry =
                service().fail(firstAttempt, "timeout");

        assertEquals(MessageHandleStatus.RETRY, retry.status());

        ResumeAnalysisExecutionBO secondAttempt =
                new ResumeAnalysisExecutionBO(
                        20L, 10L, 1L, "resume text", 2);
        when(resumesMapper.failAnalysis(
                any(), any(), any(), anyInt(), any(), any())).thenReturn(1);

        MessageHandleResult failed =
                service().fail(secondAttempt, "timeout");

        assertEquals(MessageHandleStatus.PERMANENT_FAILURE, failed.status());
    }

    private Resumes readyResume(Long resumeId) {
        return Resumes.builder()
                .id(resumeId)
                .userId(1L)
                .resumeText("Java backend engineer")
                .analyzeStatus(AnalyzeStatus.PENDING)
                .build();
    }

    private ResumeAnalysisStateService service() {
        return new ResumeAnalysisStateService(
                resumesMapper, resumeAnalysesMapper, localMessageApi,
                eventPublisher, new ObjectMapper());
    }
}
