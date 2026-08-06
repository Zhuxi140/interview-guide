package interview.interviewcfg.model.req;

import interview.common.enums.InterviewType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewScheduleCreateReqTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void roundNoMustBePositive() {
        InterviewScheduleCreateReq req = new InterviewScheduleCreateReq(
                2001L,
                (short) 0,
                3001L,
                OffsetDateTime.now().plusDays(1),
                60,
                InterviewType.TEXT
        );

        assertTrue(validator.validate(req).stream()
                .anyMatch(violation -> "roundNo".equals(
                        violation.getPropertyPath().toString())));
    }
}
