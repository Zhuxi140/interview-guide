package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "创建面试阶段模板请求")
public record InterviewTemplateCreateReq(
        @NotBlank(message = "模板名称不能为空")
        @Size(max = 64, message = "模板名称最长64字符")
        @Schema(description = "流程模板名称", example = "Java高级工程师面试")
        String templateName,

        @NotEmpty(message = "阶段列表不能为空")
        @Valid
        @Schema(description = "阶段节点列表")
        List<StageItem> stages
) {
    @Schema(description = "阶段节点项")
    public record StageItem(
            @NotBlank(message = "阶段编码不能为空")
            @Schema(description = "阶段编码", example = "TECH")
            String phaseCode,

            @NotBlank(message = "阶段名称不能为空")
            @Schema(description = "阶段名称", example = "技术面")
            String phaseName,

            @Schema(description = "排序序号", example = "1")
            Integer sortOrder
    ) {
    }
}
