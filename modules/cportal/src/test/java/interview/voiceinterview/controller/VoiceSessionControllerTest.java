package interview.voiceinterview.controller;

import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.voiceinterview.model.vo.EvaluationDetailVO;
import interview.voiceinterview.model.vo.EvaluationVO;
import interview.voiceinterview.model.vo.VoiceMessagePageVO;
import interview.voiceinterview.model.enums.VoiceEvaluationStatus;
import interview.voiceinterview.model.enums.VoiceInterviewPhase;
import interview.voiceinterview.model.enums.VoiceMessageType;
import interview.voiceinterview.model.vo.EvaluationDetailVO;
import interview.voiceinterview.model.vo.EvaluationVO;
import interview.voiceinterview.model.vo.VoiceMessagePageVO;
import interview.voiceinterview.model.vo.VoiceMessageVO;
import interview.voiceinterview.service.VoiceInterviewEvaluationService;
import interview.voiceinterview.service.VoiceInterviewMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceSessionControllerTest {

    @Mock
    private VoiceInterviewMessageService voiceInterviewMessageService;

    @Mock
    private VoiceInterviewEvaluationService voiceInterviewEvaluationService;

    private VoiceSessionController controller;

    @BeforeEach
    void setUp() {
        controller = new VoiceSessionController(
                voiceInterviewMessageService, voiceInterviewEvaluationService);
    }

    @Nested
    class QueryMessages {

        @Test
        void queryMessages_success() {
            VoiceMessageVO message = new VoiceMessageVO(
                    1L, "evt-1", VoiceMessageType.USER_SPEECH, VoiceInterviewPhase.TECH,
                    "你好", null, 1L, OffsetDateTime.now(), "trace-1");
            VoiceMessagePageVO page = new VoiceMessagePageVO(1L, false, List.of(message));
            when(voiceInterviewMessageService.queryMessages(10L, 0L, 50)).thenReturn(page);

            Result<VoiceMessagePageVO> result = controller.queryMessages(10L, 0L, 50);

            assertNotNull(result);
            assertEquals(1L, result.getData().nextSequence());
            assertEquals(1, result.getData().records().size());
            verify(voiceInterviewMessageService).queryMessages(10L, 0L, 50);
        }

        @Test
        void queryMessages_fail_notOwnSession() {
            when(voiceInterviewMessageService.queryMessages(10L, 0L, 50))
                    .thenThrow(new BusinessException(ErrorCode.VOICE_SESSION_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.queryMessages(10L, 0L, 50));
        }
    }

    @Nested
    class GetEvaluation {

        @Test
        void getEvaluation_success() {
            EvaluationVO evaluation = new EvaluationVO(
                    VoiceEvaluationStatus.COMPLETED,
                    1,
                    null,
                    new EvaluationDetailVO(
                            1L, 88, List.of(), List.of("表达清晰"), List.of(),
                            OffsetDateTime.now()));
            when(voiceInterviewEvaluationService.getEvaluation(10L)).thenReturn(evaluation);

            Result<EvaluationVO> result = controller.getEvaluation(10L);

            assertNotNull(result);
            assertEquals(VoiceEvaluationStatus.COMPLETED, result.getData().evaluationStatus());
            verify(voiceInterviewEvaluationService).getEvaluation(10L);
        }

        @Test
        void getEvaluation_fail_notFound() {
            when(voiceInterviewEvaluationService.getEvaluation(10L))
                    .thenThrow(new BusinessException(ErrorCode.VOICE_EVALUATION_NOT_FOUND));

            assertThrows(BusinessException.class, () -> controller.getEvaluation(10L));
        }
    }
}
