package interview.code.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * 候选人读取当前会话分配题目响应（仅公开用例可见）。
 */
@Builder
@Schema(description = "候选人会话题目响应")
public record SessionCodeQuestionVO(
        @Schema(description = "题目 ID")
        Long id,
        @Schema(description = "题目名称")
        String title,
        @Schema(description = "题目详细描述（Markdown）")
        String description,
        @Schema(description = "沙箱运行时间限制（毫秒）", example = "1000")
        Integer timeLimitMs,
        @Schema(description = "沙箱运行内存限制（MB）", example = "256")
        Integer memoryLimitMb,
        @Schema(description = "允许提交的编程语言列表", example = "[\"JAVA\", \"PYTHON\"]")
        List<String> supportedLanguages,
        @Schema(description = "公开示例用例列表；隐藏用例不返回")
        List<CodePublicExampleVO> publicExamples,
        @Schema(description = "每道题的提交次数上限；分配器未实现前为 null", example = "5")
        Integer attemptLimit
) {
}
