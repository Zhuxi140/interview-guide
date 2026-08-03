package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "创建面试阶段模板请求")
public record InterviewTemplateCreateReq(
        @NotBlank(message = "模板名称不能为空")
        @Size(max = 64, message = "模板名称最长64字符")
        @Schema(description = "流程模板名称", example = "Java高级工程师面试")
        String templateName,

        @NotEmpty(message = "阶段列表不能为空")
        @Size(max = 10, message = "阶段数量不能超过10个")
        @Valid
        @Schema(description = "阶段节点列表")
        List<StageItem> stages
) {
    @Schema(description = "阶段节点项")
    public record StageItem(
            @NotBlank(message = "阶段编码不能为空")
            @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$", message = "阶段编码必须为大写字母、数字或下划线")
            @Schema(description = "阶段编码", example = "TECH")
            String phaseCode,

            @NotBlank(message = "阶段名称不能为空")
            @Size(max = 64, message = "阶段名称最长64字符")
            @Schema(description = "阶段名称", example = "技术面")
            String phaseName,

            @NotNull(message = "排序序号不能为空")
            @Min(value = 1, message = "排序序号不能小于1")
            @Max(value = 10, message = "排序序号不能大于10")
            @Schema(description = "排序序号", example = "1")
            Integer sortOrder
    ) {
    }
}
