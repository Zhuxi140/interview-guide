package interview.job.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.job.model.enums.JobStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 岗位详情响应
 */
@Builder
public record JobDetailVO(
        Long id,
        String title,
        String jdContent,
        String department,
        String location,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        String experienceReq,
        String educationReq,
        String skillsJson,
        JobStatus status,
        Long userId,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
