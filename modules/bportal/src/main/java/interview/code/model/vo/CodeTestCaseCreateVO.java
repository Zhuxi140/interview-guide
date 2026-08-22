package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 创建测试用例响应。
 */
@Builder
@Schema(description = "创建测试用例响应")
public record CodeTestCaseCreateVO(
        @Schema(description = "用例 ID")
        Long id,
        @Schema(description = "所属题目 ID")
        Long questionId,
        @Schema(description = "是否为隐藏黑盒边界用例")
        Boolean isSecret,
        @Schema(description = "并发版本号", example = "0")
        Integer version
) {
}
