package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 公开用例逐条执行结果响应。
 */
@Builder
@Schema(description = "公开用例执行结果响应")
public record CodeCaseResultVO(
        @Schema(description = "用例序号（按用例创建顺序编号，从 1 开始）", example = "1")
        Integer caseNo,
        @Schema(description = "是否通过")
        Boolean passed,
        @Schema(description = "实际输出原文")
        String actualOutput,
        @Schema(description = "该用例执行耗时（毫秒）", example = "12")
        Integer executionTimeMs,
        @Schema(description = "该用例内存占用（MB）", example = "64")
        Integer memoryUsedMb
) {
}
