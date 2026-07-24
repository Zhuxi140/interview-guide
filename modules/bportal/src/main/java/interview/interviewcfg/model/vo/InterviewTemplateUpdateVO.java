package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "更新面试阶段模板响应")
public record InterviewTemplateUpdateVO(
        @Schema(description = "模板ID")
        Long id,

        @Schema(description = "流程模板名称")
        String templateName,

        @Schema(description = "版本号")
        Integer version,

        @Schema(description = "更新时间")
        OffsetDateTime updatedAt
) {
}
