package interview.job.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 编辑岗位响应
 */
@Builder
public record JobUpdateVO(
        Long id,
        String title,
        Integer status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
