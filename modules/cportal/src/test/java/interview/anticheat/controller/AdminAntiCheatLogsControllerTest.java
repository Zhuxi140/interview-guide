package interview.anticheat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.anticheat.model.enums.AntiCheatEventType;
import interview.anticheat.model.req.AntiCheatLogSearchReq;
import interview.anticheat.model.vo.AntiCheatEvidencePresignVO;
import interview.anticheat.model.vo.AntiCheatLogDetailVO;
import interview.anticheat.model.vo.AntiCheatLogListItemVO;
import interview.anticheat.service.AntiCheatService;
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
class AdminAntiCheatLogsControllerTest {

    @Mock
    private AntiCheatService antiCheatService;

    private AdminAntiCheatLogsController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminAntiCheatLogsController(antiCheatService);
    }

    @Nested
    class PageLogs {

        @Test
        void pageLogs_success() {
            AntiCheatLogSearchReq req = new AntiCheatLogSearchReq();
            req.setPage(1);
            req.setSize(20);
            req.setEventType(AntiCheatEventType.PAGE_BLUR);
            Page<AntiCheatLogListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(AntiCheatLogListItemVO.builder()
                    .id(1L).userId(100L).sessionId(20L)
                    .eventType(AntiCheatEventType.PAGE_BLUR)
                    .durationMs(3200L).evidenceAvailable(true).build()));
            when(antiCheatService.pageLogs(req)).thenReturn(page);

            Result<IPage<AntiCheatLogListItemVO>> result = controller.pageLogs(req);

            assertEquals(1, result.getData().getTotal());
            assertEquals(AntiCheatEventType.PAGE_BLUR,
                    result.getData().getRecords().get(0).eventType());
        }
    }

    @Nested
    class GetLogDetail {

        @Test
        void getLogDetail_success() {
            AntiCheatLogDetailVO vo = AntiCheatLogDetailVO.builder()
                    .id(1L).userId(100L).sessionId(20L)
                    .eventType(AntiCheatEventType.NO_FACE)
                    .traceId("a1b2c3d4e5f6")
                    .evidenceAvailable(false)
                    .build();
            when(antiCheatService.getLogDetail(1L)).thenReturn(vo);

            Result<AntiCheatLogDetailVO> result = controller.getLogDetail(1L);

            assertEquals("a1b2c3d4e5f6", result.getData().traceId());
            verify(antiCheatService).getLogDetail(1L);
        }

        @Test
        void getLogDetail_notFound_fail() {
            when(antiCheatService.getLogDetail(99L))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR, "防作弊日志不存在"));

            assertThrows(BusinessException.class, () -> controller.getLogDetail(99L));
        }
    }

    @Nested
    class GetEvidencePresign {

        @Test
        void getEvidencePresign_success() {
            AntiCheatEvidencePresignVO vo = AntiCheatEvidencePresignVO.builder()
                    .downloadUrl("https://rustfs.example/presigned")
                    .expiresInSeconds(300L)
                    .build();
            when(antiCheatService.getEvidencePresign(1L)).thenReturn(vo);

            Result<AntiCheatEvidencePresignVO> result = controller.getEvidencePresign(1L);

            assertNotNull(result.getData().downloadUrl());
            verify(antiCheatService).getEvidencePresign(1L);
        }

        @Test
        void getEvidencePresign_noEvidence_fail() {
            when(antiCheatService.getEvidencePresign(1L))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR, "该日志未关联证据材料"));

            assertThrows(BusinessException.class, () -> controller.getEvidencePresign(1L));
        }
    }
}
