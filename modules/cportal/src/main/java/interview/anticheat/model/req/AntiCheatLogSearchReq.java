package interview.anticheat.model.req;

import interview.anticheat.model.enums.AntiCheatEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 平台端防作弊日志分页查询请求。
 */
@Data
@Schema(description = "平台端防作弊日志分页查询请求")
public class AntiCheatLogSearchReq {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码从 1 开始")
    @Schema(description = "页码（从 1 开始）", example = "1")
    private Integer page;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数至少为 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    @Schema(description = "每页条数（1~100）", example = "20")
    private Integer size;

    @Schema(description = "事件类型筛选项；不传返回全部", example = "PAGE_BLUR")
    private AntiCheatEventType eventType;

    @Schema(description = "候选人用户 ID 筛选项；不传返回全部", example = "10001")
    private Long userId;

    @Schema(description = "面试会话 ID 筛选项；不传返回全部", example = "20001")
    private Long sessionId;
}
