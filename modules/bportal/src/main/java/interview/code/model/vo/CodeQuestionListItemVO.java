package interview.code.model.vo;

import interview.code.model.enums.CodeVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 编程题列表项响应。
 */
@Builder
@Schema(description = "编程题列表项响应")
public record CodeQuestionListItemVO(
        @Schema(description = "题目 ID")
        Long id,
        @Schema(description = "题目名称")
        String title,
        @Schema(description = "可见性：GLOBAL 平台全局题 / PRIVATE 企业私有题")
        CodeVisibility visibility,
        @Schema(description = "沙箱运行时间限制（毫秒）", example = "1000")
        Integer timeLimitMs,
        @Schema(description = "沙箱运行内存限制（MB）", example = "256")
        Integer memoryLimitMb,
        @Schema(description = "并发版本号", example = "0")
        Integer version,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
