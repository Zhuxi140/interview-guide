package interview.code.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.code.model.enums.CodeExecutionStatus;
import interview.code.model.req.SessionCodeSubmissionSearchReq;
import interview.code.model.vo.CodeSubmissionDetailVO;
import interview.code.model.vo.CodeSubmissionListItemVO;
import interview.code.model.vo.SessionCodeQuestionVO;
import interview.code.service.CodeQuestionService;
import interview.code.service.CodeSubmissionQueryService;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionCodeControllerTest {

    private static final Long SESSION_ID = 500L;

    @Mock
    private CodeQuestionService codeQuestionService;

    @Mock
    private CodeSubmissionQueryService codeSubmissionQueryService;

    private SessionCodeController controller;

    @BeforeEach
    void setUp() {
        controller = new SessionCodeController(codeQuestionService, codeSubmissionQueryService);
    }

    @Nested
    class GetSessionQuestion {

        @Test
        void getSessionQuestion_success() {
            SessionCodeQuestionVO vo = SessionCodeQuestionVO.builder()
                    .id(1L).title("两数之和").attemptLimit(null).build();
            when(codeQuestionService.getSessionQuestion(SESSION_ID, 1L)).thenReturn(vo);

            Result<SessionCodeQuestionVO> result =
                    controller.getSessionQuestion(SESSION_ID, 1L);

            assertEquals("两数之和", result.getData().title());
            verify(codeQuestionService).getSessionQuestion(SESSION_ID, 1L);
        }

        @Test
        void getSessionQuestion_fail_notOwnSession() {
            // 非本人会话读取题目按会话不存在处理，避免泄露会话内题目。
            when(codeQuestionService.getSessionQuestion(SESSION_ID, 1L))
                    .thenThrow(new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.getSessionQuestion(SESSION_ID, 1L));
        }
    }

    @Nested
    class PageMySubmissions {

        @Test
        void pageMySubmissions_success() {
            SessionCodeSubmissionSearchReq req = new SessionCodeSubmissionSearchReq();
            Page<CodeSubmissionListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(CodeSubmissionListItemVO.builder()
                    .submissionId(9L).questionId(1L)
                    .executionStatus(CodeExecutionStatus.PASS)
                    .passedCount(10L).totalCount(10L).attemptNumber(1L).build()));
            when(codeSubmissionQueryService.pageMySubmissions(SESSION_ID, req))
                    .thenReturn(page);

            Result<IPage<CodeSubmissionListItemVO>> result =
                    controller.pageMySubmissions(SESSION_ID, req);

            assertEquals(1, result.getData().getRecords().size());
            assertEquals(CodeExecutionStatus.PASS,
                    result.getData().getRecords().getFirst().executionStatus());
        }
    }

    @Nested
    class GetMySubmission {

        @Test
        void getMySubmission_success() {
            CodeSubmissionDetailVO vo = CodeSubmissionDetailVO.builder()
                    .submissionId(9L).executionStatus(CodeExecutionStatus.PASS)
                    .passedCount(10L).totalCount(10L).build();
            when(codeSubmissionQueryService.getMySubmission(SESSION_ID, 9L)).thenReturn(vo);

            Result<CodeSubmissionDetailVO> result = controller.getMySubmission(SESSION_ID, 9L);

            assertNotNull(result);
            assertEquals(9L, result.getData().submissionId());
        }

        @Test
        void getMySubmission_fail_notFound() {
            when(codeSubmissionQueryService.getMySubmission(SESSION_ID, 9L))
                    .thenThrow(new BusinessException(ErrorCode.CODE_SUBMISSION_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.getMySubmission(SESSION_ID, 9L));
        }
    }
}
