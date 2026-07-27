package interview.resume.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 简历删除结果。
 */
@Schema(description = "简历删除结果")
public record ResumeDeleteVO(
        @Schema(description = "已逻辑删除的简历 ID")
        Long id
) {
}
