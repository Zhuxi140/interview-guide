package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "面试阶段模板列表项")
public record InterviewTemplateListItemVO(
        @Schema(description = "模板ID")
        Long id,

        @Schema(description = "流程模板名称")
        String templateName,

        @Schema(description = "阶段数量")
        Integer stageCount,

        @Schema(description = "版本号")
        Integer version,

        @Schema(description = "创建时间")
        OffsetDateTime createdAt,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
