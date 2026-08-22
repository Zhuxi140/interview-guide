package interview.code.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.code.model.req.CodeQuestionCreateReq;
import interview.code.model.req.CodeQuestionUpdateReq;
import interview.code.model.req.EnterpriseCodeQuestionSearchReq;
import interview.code.model.enums.CodeVisibility;
import interview.code.model.vo.CodeQuestionCreateVO;
import interview.code.model.vo.CodeQuestionListItemVO;
import interview.code.model.vo.CodeQuestionUpdateVO;
import interview.code.service.CodeQuestionService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterpriseCodeQuestionControllerTest {

    private static final Long ENTERPRISE_ID = 100L;

    @Mock
    private CodeQuestionService codeQuestionService;

    private EnterpriseCodeQuestionController controller;

    @BeforeEach
    void setUp() {
        controller = new EnterpriseCodeQuestionController(codeQuestionService);
    }

    @Nested
    class CreateQuestion {

        @Test
        void createQuestion_success() {
            CodeQuestionCreateReq req = new CodeQuestionCreateReq();
            req.setTitle("反转链表");
            CodeQuestionCreateVO vo = CodeQuestionCreateVO.builder()
                    .id(2L).title("反转链表")
                    .visibility(CodeVisibility.PRIVATE).version(0)
                    .createdAt(OffsetDateTime.now()).build();
            when(codeQuestionService.createEnterpriseQuestion(ENTERPRISE_ID, req))
                    .thenReturn(vo);

            Result<CodeQuestionCreateVO> result =
                    controller.createQuestion(ENTERPRISE_ID, req);

            assertEquals(CodeVisibility.PRIVATE, result.getData().visibility());
            verify(codeQuestionService).createEnterpriseQuestion(ENTERPRISE_ID, req);
        }

        @Test
        void createQuestion_fail_notEnterpriseMember() {
            CodeQuestionCreateReq req = new CodeQuestionCreateReq();
            when(codeQuestionService.createEnterpriseQuestion(ENTERPRISE_ID, req))
                    .thenThrow(new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG));

            assertThrows(BusinessException.class,
                    () -> controller.createQuestion(ENTERPRISE_ID, req));
        }
    }

    @Nested
    class PageQuestions {

        @Test
        void pageQuestions_success() {
            EnterpriseCodeQuestionSearchReq req = new EnterpriseCodeQuestionSearchReq();
            Page<CodeQuestionListItemVO> page = new Page<>(1, 20, 2);
            page.setRecords(List.of(
                    CodeQuestionListItemVO.builder()
                            .id(1L).visibility(CodeVisibility.GLOBAL).build(),
                    CodeQuestionListItemVO.builder()
                            .id(2L).visibility(CodeVisibility.PRIVATE).build()));
            when(codeQuestionService.pageEnterpriseQuestions(ENTERPRISE_ID, req))
                    .thenReturn(page);

            Result<IPage<CodeQuestionListItemVO>> result =
                    controller.pageQuestions(ENTERPRISE_ID, req);

            assertEquals(2, result.getData().getRecords().size());
        }
    }

    @Nested
    class UpdateQuestion {

        @Test
        void updateQuestion_success() {
            CodeQuestionUpdateReq req = new CodeQuestionUpdateReq();
            req.setExpectedVersion(0);
            when(codeQuestionService.updateEnterpriseQuestion(ENTERPRISE_ID, 2L, req))
                    .thenReturn(new CodeQuestionUpdateVO(2L, 1, OffsetDateTime.now()));

            Result<CodeQuestionUpdateVO> result =
                    controller.updateQuestion(ENTERPRISE_ID, 2L, req);

            assertEquals(1, result.getData().version());
        }

        @Test
        void updateQuestion_fail_globalQuestion() {
            // 全局题只能由平台管理端维护，企业侧更新全局题按不存在处理。
            CodeQuestionUpdateReq req = new CodeQuestionUpdateReq();
            req.setExpectedVersion(0);
            when(codeQuestionService.updateEnterpriseQuestion(ENTERPRISE_ID, 1L, req))
                    .thenThrow(new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.updateQuestion(ENTERPRISE_ID, 1L, req));
        }
    }

    @Nested
    class DeleteQuestion {

        @Test
        void deleteQuestion_success() {
            Result<Void> result = controller.deleteQuestion(ENTERPRISE_ID, 2L, 0);

            assertNotNull(result);
            verify(codeQuestionService).deleteEnterpriseQuestion(ENTERPRISE_ID, 2L, 0);
        }
    }
}
