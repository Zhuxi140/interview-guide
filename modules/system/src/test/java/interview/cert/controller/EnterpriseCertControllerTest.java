package interview.cert.controller;

import interview.cert.model.req.EnterpriseCertSubmitReq;
import interview.cert.model.req.EnterpriseCertUploadTicketReq;
import interview.cert.model.vo.EnterpriseCertStatusVO;
import interview.cert.model.vo.EnterpriseCertSubmitVO;
import interview.cert.model.vo.EnterpriseCertUploadTicketVO;
import interview.cert.model.enums.CertStatus;
import interview.cert.service.EnterpriseCertService;
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
class EnterpriseCertControllerTest {

    @Mock
    private EnterpriseCertService enterpriseCertService;

    private EnterpriseCertController controller;

    @BeforeEach
    void setUp() {
        controller = new EnterpriseCertController(enterpriseCertService);
    }

    @Nested
    class CreateLicenseUploadTicket {

        @Test
        void createLicenseUploadTicket_success() {
            EnterpriseCertUploadTicketReq req = new EnterpriseCertUploadTicketReq();
            req.setFileName("business-license.jpg");
            req.setContentType("image/jpeg");
            req.setFileSize(2048L);
            req.setSha256("3f2a9d8c7b6e5f4a3d2c1b0a99887766554433221100ffeeddccbbaa99887766");
            EnterpriseCertUploadTicketVO vo = EnterpriseCertUploadTicketVO.builder()
                    .materialToken("ENTERPRISE_CERT/2026/08/22/abc_business-license.jpg")
                    .expiresInSeconds(300L)
                    .build();
            when(enterpriseCertService.createLicenseUploadTicket(10L, req)).thenReturn(vo);

            Result<EnterpriseCertUploadTicketVO> result =
                    controller.createLicenseUploadTicket(10L, req);

            assertEquals("ENTERPRISE_CERT/2026/08/22/abc_business-license.jpg",
                    result.getData().materialToken());
            verify(enterpriseCertService).createLicenseUploadTicket(10L, req);
        }

        @Test
        void createLicenseUploadTicket_notOwner_fail() {
            EnterpriseCertUploadTicketReq req = new EnterpriseCertUploadTicketReq();
            when(enterpriseCertService.createLicenseUploadTicket(10L, req))
                    .thenThrow(new BusinessException(ErrorCode.NOT_ENTERPRISE_OWNER));

            assertThrows(BusinessException.class,
                    () -> controller.createLicenseUploadTicket(10L, req));
        }
    }

    @Nested
    class SubmitCertification {

        @Test
        void submitCertification_success() {
            EnterpriseCertSubmitReq req = new EnterpriseCertSubmitReq();
            req.setCreditCode("91330100MA27X8901X");
            req.setLegalPerson("张三");
            req.setLicenseMaterialToken("ENTERPRISE_CERT/2026/08/22/abc_business-license.jpg");
            EnterpriseCertSubmitVO vo = EnterpriseCertSubmitVO.builder()
                    .id(1L)
                    .enterpriseId(10L)
                    .auditStatus(CertStatus.PENDING)
                    .build();
            when(enterpriseCertService.submitCertification(10L, req)).thenReturn(vo);

            Result<EnterpriseCertSubmitVO> result = controller.submitCertification(10L, req);

            assertEquals(CertStatus.PENDING, result.getData().auditStatus());
            verify(enterpriseCertService).submitCertification(10L, req);
        }

        @Test
        void submitCertification_invalidMaterial_fail() {
            EnterpriseCertSubmitReq req = new EnterpriseCertSubmitReq();
            when(enterpriseCertService.submitCertification(10L, req))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                            "营业执照材料无效或尚未上传完成"));

            assertThrows(BusinessException.class,
                    () -> controller.submitCertification(10L, req));
        }
    }

    @Nested
    class GetCertificationStatus {

        @Test
        void getCertificationStatus_notSubmitted() {
            when(enterpriseCertService.getCertificationStatus(10L)).thenReturn(
                    EnterpriseCertStatusVO.builder()
                            .auditStatus(CertStatus.NOT_SUBMITTED)
                            .build());

            Result<EnterpriseCertStatusVO> result = controller.getCertificationStatus(10L);

            assertEquals(CertStatus.NOT_SUBMITTED, result.getData().auditStatus());
        }
    }
}
