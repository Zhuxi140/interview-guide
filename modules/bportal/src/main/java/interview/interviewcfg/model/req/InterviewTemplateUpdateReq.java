package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "更新面试阶段模板请求")
public record InterviewTemplateUpdateReq(
        @Size(max = 64, message = "模板名称最长64字符")
        @Schema(description = "流程模板名称", example = "Java高级工程师面试 v2")
        String templateName,

        @Valid
        @Size(max = 10, message = "阶段数量不能超过10个")
        @Schema(description = "阶段节点列表（全量替换）")
        List<InterviewTemplateCreateReq.StageItem> stages,

        @NotNull(message = "乐观锁版本号不能为空")
        @Schema(description = "期望版本号（乐观锁）", example = "1")
        Integer expectedVersion
) {
}
