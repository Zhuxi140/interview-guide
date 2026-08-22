package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 公开示例用例响应（候选人视角，隐藏用例不返回）。
 */
@Builder
@Schema(description = "公开示例用例响应")
public record CodePublicExampleVO(
        @Schema(description = "输入测试用例参数原文")
        String input,
        @Schema(description = "期望的标准判定输出结果")
        String expectedOutput
) {
}
