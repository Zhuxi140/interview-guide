package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "AI 推荐排期项")
public record AiSuggestionItemVO(
        @Schema(description = "推荐的面试时间")
        OffsetDateTime interviewTime,

        @Schema(description = "面试官用户ID")
        Long interviewerUserId,

        @Schema(description = "推荐理由", example = "面试官 10:00-12:00 有空")
        String reason
) {
}
