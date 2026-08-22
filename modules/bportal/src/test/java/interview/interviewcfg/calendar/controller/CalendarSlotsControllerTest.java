package interview.interviewcfg.calendar.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.interviewcfg.calendar.model.enums.CalendarSlotStatus;
import interview.interviewcfg.calendar.model.vo.CalendarSlotListItemVO;
import interview.interviewcfg.calendar.service.CalendarSlotService;
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
class CalendarSlotsControllerTest {

    @Mock
    private CalendarSlotService calendarSlotService;

    private CalendarSlotsController controller;

    @BeforeEach
    void setUp() {
        controller = new CalendarSlotsController(calendarSlotService);
    }

    @Nested
    class ListSlots {

        @Test
        void listSlots_success() {
            Page<CalendarSlotListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new CalendarSlotListItemVO(
                    1L, 100L, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                    CalendarSlotStatus.AVAILABLE, 0, OffsetDateTime.now())));
            when(calendarSlotService.pageSlots(9L, 1, 20, 100L,
                    "2026-08-01T00:00:00+08:00", "2026-08-31T00:00:00+08:00",
                    CalendarSlotStatus.AVAILABLE)).thenReturn(page);

            Result<IPage<CalendarSlotListItemVO>> result = controller.listSlots(
                    9L, 1, 20, 100L, "2026-08-01T00:00:00+08:00",
                    "2026-08-31T00:00:00+08:00", CalendarSlotStatus.AVAILABLE);

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
            verify(calendarSlotService).pageSlots(9L, 1, 20, 100L,
                    "2026-08-01T00:00:00+08:00", "2026-08-31T00:00:00+08:00",
                    CalendarSlotStatus.AVAILABLE);
        }

        @Test
        void listSlots_invalidPageParams_fail_serviceThrows() {
            when(calendarSlotService.pageSlots(9L, 0, 20, null, null, null, null))
                    .thenThrow(new BusinessException(ErrorCode.PAGE_PARAM_INVALID));

            assertThrows(BusinessException.class,
                    () -> controller.listSlots(9L, 0, 20, null, null, null, null));
        }
    }

    @Nested
    class DeleteMySlot {

        @Test
        void deleteMySlot_success() {
            Result<Void> result = controller.deleteMySlot(9L, 1L, 0);

            assertNotNull(result);
            verify(calendarSlotService).deleteMySlot(9L, 1L, 0);
        }

        @Test
        void deleteMySlot_allocatedSlot_fail_serviceThrows() {
            org.mockito.Mockito.doThrow(
                            new BusinessException(ErrorCode.PARAM_VALID_ERROR, "已被排期占用的时段不允许删除"))
                    .when(calendarSlotService).deleteMySlot(9L, 1L, 0);

            assertThrows(BusinessException.class,
                    () -> controller.deleteMySlot(9L, 1L, 0));
        }

        @Test
        void deleteMySlot_versionConflict_fail_serviceThrows() {
            org.mockito.Mockito.doThrow(
                            new BusinessException(ErrorCode.PARAM_VALID_ERROR, "时段已被修改，请刷新后重试"))
                    .when(calendarSlotService).deleteMySlot(9L, 1L, 3);

            assertThrows(BusinessException.class,
                    () -> controller.deleteMySlot(9L, 1L, 3));
        }
    }
}
