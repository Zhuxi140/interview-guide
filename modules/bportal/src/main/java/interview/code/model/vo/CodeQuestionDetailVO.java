package interview.code.model.vo;

import interview.code.model.enums.CodeVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 编程题详情响应（含测试用例，仅管理端可见完整用例）。
 */
@Builder
@Schema(description = "编程题详情响应")
public record CodeQuestionDetailVO(
        @Schema(description = "题目 ID")
        Long id,
        @Schema(description = "题目名称")
        String title,
        @Schema(description = "题目详细描述（Markdown）")
        String description,
        @Schema(description = "可见性：GLOBAL 平台全局题 / PRIVATE 企业私有题")
        CodeVisibility visibility,
        @Schema(description = "沙箱运行时间限制（毫秒）", example = "1000")
        Integer timeLimitMs,
        @Schema(description = "沙箱运行内存限制（MB）", example = "256")
        Integer memoryLimitMb,
        @Schema(description = "允许提交的编程语言列表", example = "[\"JAVA\", \"PYTHON\"]")
        List<String> supportedLanguages,
        @Schema(description = "并发版本号", example = "0")
        Integer version,
        @Schema(description = "测试用例列表")
        List<CodeTestCaseItemVO> testCases,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt,
        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
