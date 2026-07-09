package interview.job.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.job.model.enums.JobStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 岗位列表项响应
 */
@Builder
public record JobListItemVO(
        Long id,
        String title,
        String department,
        String location,
        JobStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        Integer candidateCount
) {
}
