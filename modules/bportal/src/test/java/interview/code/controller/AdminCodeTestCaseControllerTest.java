package interview.code.controller;

import interview.code.model.req.CodeTestCaseCreateReq;
import interview.code.model.req.CodeTestCaseUpdateReq;
import interview.code.model.vo.CodeTestCaseCreateVO;
import interview.code.model.vo.CodeTestCaseUpdateVO;
import interview.code.service.CodeTestCaseService;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCodeTestCaseControllerTest {

    @Mock
    private CodeTestCaseService codeTestCaseService;

    private AdminCodeTestCaseController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminCodeTestCaseController(codeTestCaseService);
    }

    @Nested
    class CreateTestCase {

        @Test
        void createTestCase_success() {
            CodeTestCaseCreateReq req = new CodeTestCaseCreateReq();
            req.setInputCase("1 2");
            req.setExpectedOutput("3");
            CodeTestCaseCreateVO vo = CodeTestCaseCreateVO.builder()
                    .id(10L).questionId(1L).isSecret(false).version(0).build();
            when(codeTestCaseService.createPlatformTestCase(1L, req)).thenReturn(vo);

            Result<CodeTestCaseCreateVO> result = controller.createTestCase(1L, req);

            assertEquals(10L, result.getData().id());
            verify(codeTestCaseService).createPlatformTestCase(1L, req);
        }

        @Test
        void createTestCase_fail_questionNotFound() {
            CodeTestCaseCreateReq req = new CodeTestCaseCreateReq();
            when(codeTestCaseService.createPlatformTestCase(1L, req))
                    .thenThrow(new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND));

            assertThrows(BusinessException.class, () -> controller.createTestCase(1L, req));
        }
    }

    @Nested
    class UpdateTestCase {

        @Test
        void updateTestCase_success() {
            CodeTestCaseUpdateReq req = new CodeTestCaseUpdateReq();
            req.setExpectedVersion(0);
            when(codeTestCaseService.updatePlatformTestCase(1L, 10L, req))
                    .thenReturn(new CodeTestCaseUpdateVO(10L, 1, OffsetDateTime.now()));

            Result<CodeTestCaseUpdateVO> result = controller.updateTestCase(1L, 10L, req);

            assertEquals(1, result.getData().version());
        }
    }

    @Nested
    class DeleteTestCase {

        @Test
        void deleteTestCase_success() {
            Result<Void> result = controller.deleteTestCase(1L, 10L, 0);

            assertNotNull(result);
            verify(codeTestCaseService).deletePlatformTestCase(1L, 10L, 0);
        }

        @Test
        void deleteTestCase_fail_caseNotFound() {
            doThrow(new BusinessException(ErrorCode.CODE_TEST_CASE_NOT_FOUND))
                    .when(codeTestCaseService).deletePlatformTestCase(1L, 10L, 0);

            assertThrows(BusinessException.class,
                    () -> controller.deleteTestCase(1L, 10L, 0));
        }
    }
}
