package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 更新测试用例响应。
 */
@Builder
@Schema(description = "更新测试用例响应")
public record CodeTestCaseUpdateVO(
        @Schema(description = "用例 ID")
        Long id,
        @Schema(description = "更新后的并发版本号", example = "1")
        Integer version,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
