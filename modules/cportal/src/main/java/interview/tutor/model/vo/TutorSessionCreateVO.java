package interview.tutor.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 创建 AI 答疑会话结果。
 */
@Schema(description = "创建 AI 答疑会话结果")
public record TutorSessionCreateVO(
        @Schema(description = "会话 ID")
        Long id,
        @Schema(description = "会话标题")
        String sessionTitle,
        @Schema(description = "关联面评报告 ID")
        Long associatedReportId,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
