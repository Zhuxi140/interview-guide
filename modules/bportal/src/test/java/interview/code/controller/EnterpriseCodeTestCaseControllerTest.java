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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterpriseCodeTestCaseControllerTest {

    private static final Long ENTERPRISE_ID = 100L;

    @Mock
    private CodeTestCaseService codeTestCaseService;

    private EnterpriseCodeTestCaseController controller;

    @BeforeEach
    void setUp() {
        controller = new EnterpriseCodeTestCaseController(codeTestCaseService);
    }

    @Nested
    class CreateTestCase {

        @Test
        void createTestCase_success() {
            CodeTestCaseCreateReq req = new CodeTestCaseCreateReq();
            req.setInputCase("[]");
            req.setExpectedOutput("[]");
            when(codeTestCaseService.createEnterpriseTestCase(ENTERPRISE_ID, 2L, req))
                    .thenReturn(CodeTestCaseCreateVO.builder()
                            .id(11L).questionId(2L).isSecret(true).version(0).build());

            Result<CodeTestCaseCreateVO> result =
                    controller.createTestCase(ENTERPRISE_ID, 2L, req);

            assertEquals(Boolean.TRUE, result.getData().isSecret());
            verify(codeTestCaseService).createEnterpriseTestCase(ENTERPRISE_ID, 2L, req);
        }

        @Test
        void createTestCase_fail_questionNotOwned() {
            // 企业只能为本企业私有题维护用例，全局题或他企题按不存在处理。
            CodeTestCaseCreateReq req = new CodeTestCaseCreateReq();
            when(codeTestCaseService.createEnterpriseTestCase(ENTERPRISE_ID, 1L, req))
                    .thenThrow(new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.createTestCase(ENTERPRISE_ID, 1L, req));
        }
    }

    @Nested
    class UpdateTestCase {

        @Test
        void updateTestCase_success() {
            CodeTestCaseUpdateReq req = new CodeTestCaseUpdateReq();
            req.setExpectedVersion(1);
            when(codeTestCaseService.updateEnterpriseTestCase(ENTERPRISE_ID, 2L, 11L, req))
                    .thenReturn(new CodeTestCaseUpdateVO(11L, 2, OffsetDateTime.now()));

            Result<CodeTestCaseUpdateVO> result =
                    controller.updateTestCase(ENTERPRISE_ID, 2L, 11L, req);

            assertEquals(2, result.getData().version());
        }
    }

    @Nested
    class DeleteTestCase {

        @Test
        void deleteTestCase_success() {
            Result<Void> result = controller.deleteTestCase(ENTERPRISE_ID, 2L, 11L, 1);

            assertNotNull(result);
            verify(codeTestCaseService).deleteEnterpriseTestCase(ENTERPRISE_ID, 2L, 11L, 1);
        }
    }
}
