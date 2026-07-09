package interview.job.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 发布岗位响应
 */
@Builder
public record JobCreateVO(
        Long id,
        String title,
        Integer status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
