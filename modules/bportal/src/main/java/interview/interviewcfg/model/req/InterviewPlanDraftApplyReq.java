package interview.interviewcfg.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "应用 Agent 面试编排草案请求")
public record InterviewPlanDraftApplyReq(
        @NotNull(message = "乐观锁版本号不能为空")
        @Min(value = 0, message = "乐观锁版本号不能小于0")
        @Schema(description = "客户端读取到的草案版本", example = "1")
        Integer expectedVersion,

        @Size(max = 20, message = "排期建议不能超过20条")
        @Schema(description = "选中的排期建议ID；为空时由服务端按草案默认选择")
        List<@NotBlank(message = "排期建议ID不能为空") String> selectedSuggestionIds
) {
}
