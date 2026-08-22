package interview.anticheat.controller;

import interview.anticheat.model.req.AntiCheatEvidenceUploadTicketReq;
import interview.anticheat.model.vo.AntiCheatEvidenceUploadTicketVO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionAntiCheatControllerTest {

    @Mock
    private AntiCheatService antiCheatService;

    private SessionAntiCheatController controller;

    @BeforeEach
    void setUp() {
        controller = new SessionAntiCheatController(antiCheatService);
    }

    @Nested
    class CreateEvidenceUploadTicket {

        @Test
        void createEvidenceUploadTicket_success() {
            AntiCheatEvidenceUploadTicketReq req = new AntiCheatEvidenceUploadTicketReq();
            req.setEventId("evt-3f2a9d8c7b6e");
            req.setFileName("page-blur-001.jpg");
            req.setContentType("image/jpeg");
            req.setFileSize(512L);
            req.setSha256("3f2a9d8c7b6e5f4a3d2c1b0a99887766554433221100ffeeddccbbaa99887766");
            AntiCheatEvidenceUploadTicketVO vo = AntiCheatEvidenceUploadTicketVO.builder()
                    .evidenceMaterialToken("ATTACHMENT/2026/08/22/abc_page-blur-001.jpg")
                    .expiresInSeconds(300L)
                    .build();
            when(antiCheatService.createEvidenceUploadTicket(20L, req)).thenReturn(vo);

            Result<AntiCheatEvidenceUploadTicketVO> result =
                    controller.createEvidenceUploadTicket(20L, req);

            assertEquals("ATTACHMENT/2026/08/22/abc_page-blur-001.jpg",
                    result.getData().evidenceMaterialToken());
            verify(antiCheatService).createEvidenceUploadTicket(20L, req);
        }

        @Test
        void createEvidenceUploadTicket_sessionNotFound_fail() {
            AntiCheatEvidenceUploadTicketReq req = new AntiCheatEvidenceUploadTicketReq();
            when(antiCheatService.createEvidenceUploadTicket(99L, req))
                    .thenThrow(new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.createEvidenceUploadTicket(99L, req));
        }

        @Test
        void createEvidenceUploadTicket_notParticipant_fail() {
            AntiCheatEvidenceUploadTicketReq req = new AntiCheatEvidenceUploadTicketReq();
            when(antiCheatService.createEvidenceUploadTicket(20L, req))
                    .thenThrow(new BusinessException(ErrorCode.USER_NOT_PARTICIPANT));

            assertThrows(BusinessException.class,
                    () -> controller.createEvidenceUploadTicket(20L, req));
        }
    }
}
