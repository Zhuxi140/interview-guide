package interview.interviewcfg.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "删除面试模板响应")
public record InterviewTemplateDeleteVO(
        @Schema(description = "提示信息", example = "删除成功")
        String message
) {
}
