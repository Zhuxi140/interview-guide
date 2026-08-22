package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 测试用例列表项响应（管理端题目详情内嵌）。
 */
@Builder
@Schema(description = "测试用例列表项响应")
public record CodeTestCaseItemVO(
        @Schema(description = "用例 ID")
        Long id,
        @Schema(description = "输入测试用例参数原文")
        String inputCase,
        @Schema(description = "期望的标准判定输出结果")
        String expectedOutput,
        @Schema(description = "是否为隐藏黑盒边界用例")
        Boolean isSecret,
        @Schema(description = "并发版本号", example = "0")
        Integer version
) {
}
