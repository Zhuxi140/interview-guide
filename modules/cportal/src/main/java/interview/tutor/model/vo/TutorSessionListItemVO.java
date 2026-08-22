package interview.tutor.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * AI 答疑会话列表项。
 */
@Schema(description = "AI 答疑会话列表项")
public record TutorSessionListItemVO(
        @Schema(description = "会话 ID")
        Long id,
        @Schema(description = "会话标题")
        String sessionTitle,
        @Schema(description = "关联面评报告 ID")
        Long associatedReportId,
        @Schema(description = "会话内消息数量")
        Long messageCount,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt
) {
}
