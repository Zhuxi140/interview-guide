package interview.resume.controller;

import interview.common.constant.Result;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.vo.ResumeAnalyzeTriggerVO;
import interview.resume.service.ResumesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumesControllerContractTest {

    @Mock
    private ResumesService resumesService;

    @Test
    void analyzeResume_shouldReturnAcceptedTaskAndForwardIdempotencyKey() throws Exception {
        ResumeAnalyzeTriggerVO vo =
                new ResumeAnalyzeTriggerVO(20L, 10L, AnalyzeStatus.PENDING);
        when(resumesService.analyzeResume(10L, "idem-1")).thenReturn(vo);
        ResumesController controller = new ResumesController(resumesService);

        Result<ResumeAnalyzeTriggerVO> result = controller.analyzeResume(10L, "idem-1");

        assertEquals(vo, result.getData());
        verify(resumesService).analyzeResume(10L, "idem-1");
        Method method = ResumesController.class.getMethod(
                "analyzeResume", Long.class, String.class);
        assertEquals(HttpStatus.ACCEPTED,
                method.getAnnotation(ResponseStatus.class).value());
    }
}
