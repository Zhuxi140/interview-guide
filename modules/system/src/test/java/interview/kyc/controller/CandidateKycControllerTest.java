package interview.kyc.controller;

import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.kyc.model.enums.KycMaterialType;
import interview.kyc.model.enums.KycStatus;
import interview.kyc.model.req.KycMaterialUploadTicketReq;
import interview.kyc.model.vo.KycMaterialUploadTicketVO;
import interview.kyc.model.vo.KycStatusVO;
import interview.kyc.service.KycService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateKycControllerTest {

    @Mock
    private KycService kycService;

    private CandidateKycController controller;

    @BeforeEach
    void setUp() {
        controller = new CandidateKycController(kycService);
    }

    @Nested
    class CreateMaterialUploadTicket {

        @Test
        void createMaterialUploadTicket_success() {
            KycMaterialUploadTicketReq req = new KycMaterialUploadTicketReq();
            req.setMaterialType(KycMaterialType.ID_CARD_FRONT);
            req.setFileName("id-card-front.jpg");
            req.setContentType("image/jpeg");
            req.setFileSize(1024L);
            req.setSha256("3f2a9d8c7b6e5f4a3d2c1b0a99887766554433221100ffeeddccbbaa99887766");
            KycMaterialUploadTicketVO vo = KycMaterialUploadTicketVO.builder()
                    .materialToken("KYC_ID_CARD/2026/08/22/abc_id-card-front.jpg")
                    .expiresInSeconds(300L)
                    .build();
            when(kycService.createMaterialUploadTicket(req)).thenReturn(vo);

            Result<KycMaterialUploadTicketVO> result =
                    controller.createMaterialUploadTicket(req);

            assertNotNull(result);
            assertEquals("KYC_ID_CARD/2026/08/22/abc_id-card-front.jpg",
                    result.getData().materialToken());
            verify(kycService).createMaterialUploadTicket(req);
        }

        @Test
        void createMaterialUploadTicket_fail_serviceThrows() {
            KycMaterialUploadTicketReq req = new KycMaterialUploadTicketReq();
            req.setMaterialType(KycMaterialType.FACE_LIVENESS);
            when(kycService.createMaterialUploadTicket(req))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                            "不支持的实名材料类型"));

            assertThrows(BusinessException.class,
                    () -> controller.createMaterialUploadTicket(req));
        }
    }

    @Nested
    class GetMyKycStatus {

        @Test
        void getMyKycStatus_submitted() {
            KycStatusVO vo = KycStatusVO.builder()
                    .id(1L)
                    .authStatus(KycStatus.PENDING)
                    .build();
            when(kycService.getMyKycStatus()).thenReturn(vo);

            Result<KycStatusVO> result = controller.getMyKycStatus();

            assertEquals(KycStatus.PENDING, result.getData().authStatus());
            verify(kycService).getMyKycStatus();
        }

        @Test
        void getMyKycStatus_notSubmitted() {
            when(kycService.getMyKycStatus()).thenReturn(
                    KycStatusVO.builder().authStatus(KycStatus.NOT_SUBMITTED).build());

            Result<KycStatusVO> result = controller.getMyKycStatus();

            assertEquals(KycStatus.NOT_SUBMITTED, result.getData().authStatus());
        }
    }
}
