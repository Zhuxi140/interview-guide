package interview.compliance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.compliance.model.enums.SensitiveAction;
import interview.compliance.model.enums.SensitiveCategory;
import interview.compliance.model.req.SensitiveWordCreateReq;
import interview.compliance.model.req.SensitiveWordSearchReq;
import interview.compliance.model.req.SensitiveWordUpdateReq;
import interview.compliance.model.vo.SensitiveWordCreateVO;
import interview.compliance.model.vo.SensitiveWordListItemVO;
import interview.compliance.model.vo.SensitiveWordUpdateVO;
import interview.compliance.service.SensitiveWordService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensitiveWordsControllerTest {

    @Mock
    private SensitiveWordService sensitiveWordService;

    private SensitiveWordsController controller;

    @BeforeEach
    void setUp() {
        controller = new SensitiveWordsController(sensitiveWordService);
    }

    @Nested
    class PageSensitiveWords {

        @Test
        void pageSensitiveWords_success() {
            SensitiveWordSearchReq req = new SensitiveWordSearchReq();
            req.setPage(1);
            req.setSize(20);
            req.setCategory(SensitiveCategory.CHEAT);
            Page<SensitiveWordListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(SensitiveWordListItemVO.builder()
                    .id(1L).word("外挂").category(SensitiveCategory.CHEAT)
                    .actionType(SensitiveAction.BLOCK).version(0).build()));
            when(sensitiveWordService.pageSensitiveWords(req)).thenReturn(page);

            Result<IPage<SensitiveWordListItemVO>> result = controller.pageSensitiveWords(req);

            assertEquals(1, result.getData().getTotal());
            assertEquals("外挂", result.getData().getRecords().get(0).word());
        }
    }

    @Nested
    class CreateSensitiveWord {

        @Test
        void createSensitiveWord_success() {
            SensitiveWordCreateReq req = new SensitiveWordCreateReq();
            req.setWord("外挂");
            req.setCategory(SensitiveCategory.CHEAT);
            SensitiveWordCreateVO vo = SensitiveWordCreateVO.builder()
                    .id(1L).word("外挂")
                    .category(SensitiveCategory.CHEAT)
                    .actionType(SensitiveAction.BLOCK)
                    .build();
            when(sensitiveWordService.createSensitiveWord(req)).thenReturn(vo);

            Result<SensitiveWordCreateVO> result = controller.createSensitiveWord(req);

            assertEquals(SensitiveAction.BLOCK, result.getData().actionType());
            verify(sensitiveWordService).createSensitiveWord(req);
        }

        @Test
        void createSensitiveWord_duplicated_fail() {
            SensitiveWordCreateReq req = new SensitiveWordCreateReq();
            req.setWord("外挂");
            req.setCategory(SensitiveCategory.CHEAT);
            when(sensitiveWordService.createSensitiveWord(req))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR, "敏感词已存在"));

            assertThrows(BusinessException.class, () -> controller.createSensitiveWord(req));
        }
    }

    @Nested
    class UpdateSensitiveWord {

        @Test
        void updateSensitiveWord_success() {
            SensitiveWordUpdateReq req = new SensitiveWordUpdateReq();
            req.setActionType(SensitiveAction.ALERT);
            req.setExpectedVersion(0);
            SensitiveWordUpdateVO vo = SensitiveWordUpdateVO.builder()
                    .id(1L).word("外挂")
                    .category(SensitiveCategory.CHEAT)
                    .actionType(SensitiveAction.ALERT)
                    .version(1)
                    .build();
            when(sensitiveWordService.updateSensitiveWord(1L, req)).thenReturn(vo);

            Result<SensitiveWordUpdateVO> result = controller.updateSensitiveWord(1L, req);

            assertEquals(1, result.getData().version());
            verify(sensitiveWordService).updateSensitiveWord(1L, req);
        }

        @Test
        void updateSensitiveWord_versionConflict_fail() {
            SensitiveWordUpdateReq req = new SensitiveWordUpdateReq();
            req.setExpectedVersion(0);
            when(sensitiveWordService.updateSensitiveWord(1L, req))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                            "敏感词已被其他请求修改，请刷新后重试"));

            assertThrows(BusinessException.class,
                    () -> controller.updateSensitiveWord(1L, req));
        }
    }

    @Nested
    class DeleteSensitiveWord {

        @Test
        void deleteSensitiveWord_success() {
            Result<Void> result = controller.deleteSensitiveWord(1L, 3);

            assertNotNull(result);
            verify(sensitiveWordService).deleteSensitiveWord(1L, 3);
        }

        @Test
        void deleteSensitiveWord_conflict_fail() {
            doThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "敏感词已被其他请求修改，请刷新后重试"))
                    .when(sensitiveWordService).deleteSensitiveWord(1L, 3);

            assertThrows(BusinessException.class,
                    () -> controller.deleteSensitiveWord(1L, 3));
        }
    }
}
