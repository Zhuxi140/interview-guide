package interview.code.controller;

import interview.code.model.req.AdminCodeQuestionSearchReq;
import interview.code.model.req.CodeQuestionCreateReq;
import interview.code.model.req.CodeQuestionUpdateReq;
import interview.code.model.enums.CodeVisibility;
import interview.code.model.vo.CodeQuestionCreateVO;
import interview.code.model.vo.CodeQuestionDetailVO;
import interview.code.model.vo.CodeQuestionListItemVO;
import interview.code.model.vo.CodeQuestionUpdateVO;
import interview.code.service.CodeQuestionService;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
class AdminCodeQuestionControllerTest {

    @Mock
    private CodeQuestionService codeQuestionService;

    private AdminCodeQuestionController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminCodeQuestionController(codeQuestionService);
    }

    @Nested
    class CreateQuestion {

        @Test
        void createQuestion_success() {
            CodeQuestionCreateReq req = new CodeQuestionCreateReq();
            req.setTitle("两数之和");
            CodeQuestionCreateVO vo = CodeQuestionCreateVO.builder()
                    .id(1L).title("两数之和")
                    .visibility(CodeVisibility.GLOBAL).version(0)
                    .createdAt(OffsetDateTime.now()).build();
            when(codeQuestionService.createPlatformQuestion(req)).thenReturn(vo);

            Result<CodeQuestionCreateVO> result = controller.createQuestion(req);

            assertNotNull(result);
            assertEquals(CodeVisibility.GLOBAL, result.getData().visibility());
            verify(codeQuestionService).createPlatformQuestion(req);
        }
    }

    @Nested
    class PageQuestions {

        @Test
        void pageQuestions_success() {
            AdminCodeQuestionSearchReq req = new AdminCodeQuestionSearchReq();
            Page<CodeQuestionListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(CodeQuestionListItemVO.builder()
                    .id(1L).title("两数之和").visibility(CodeVisibility.GLOBAL).build()));
            when(codeQuestionService.pagePlatformQuestions(req)).thenReturn(page);

            Result<IPage<CodeQuestionListItemVO>> result = controller.pageQuestions(req);

            assertEquals(1, result.getData().getTotal());
            assertEquals(1, result.getData().getRecords().size());
        }
    }

    @Nested
    class GetQuestion {

        @Test
        void getQuestion_success() {
            CodeQuestionDetailVO vo = CodeQuestionDetailVO.builder()
                    .id(1L).title("两数之和").visibility(CodeVisibility.GLOBAL).build();
            when(codeQuestionService.getPlatformQuestionDetail(1L)).thenReturn(vo);

            Result<CodeQuestionDetailVO> result = controller.getQuestion(1L);

            assertEquals("两数之和", result.getData().title());
        }

        @Test
        void getQuestion_fail_notFound() {
            when(codeQuestionService.getPlatformQuestionDetail(1L))
                    .thenThrow(new BusinessException(ErrorCode.CODE_QUESTION_NOT_FOUND));

            assertThrows(BusinessException.class, () -> controller.getQuestion(1L));
        }
    }

    @Nested
    class UpdateAndDelete {

        @Test
        void updateQuestion_success() {
            CodeQuestionUpdateReq req = new CodeQuestionUpdateReq();
            req.setExpectedVersion(0);
            when(codeQuestionService.updatePlatformQuestion(1L, req))
                    .thenReturn(new CodeQuestionUpdateVO(1L, 1, OffsetDateTime.now()));

            Result<CodeQuestionUpdateVO> result = controller.updateQuestion(1L, req);

            assertEquals(1, result.getData().version());
        }

        @Test
        void deleteQuestion_success() {
            Result<Void> result = controller.deleteQuestion(1L, 0);

            assertNotNull(result);
            verify(codeQuestionService).deletePlatformQuestion(1L, 0);
        }
    }
}
