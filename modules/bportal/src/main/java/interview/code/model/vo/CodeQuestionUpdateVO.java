package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 更新编程题响应。
 */
@Builder
@Schema(description = "更新编程题响应")
public record CodeQuestionUpdateVO(
        @Schema(description = "题目 ID")
        Long id,
        @Schema(description = "更新后的并发版本号", example = "1")
        Integer version,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
