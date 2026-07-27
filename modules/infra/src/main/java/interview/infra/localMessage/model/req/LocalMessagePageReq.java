package interview.infra.localMessage.model.req;

import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 本地消息分页查询参数。
 */
@Data
@Schema(description = "本地消息分页查询参数")
public class LocalMessagePageReq {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "消息状态")
    private MsgStatus status;

    @Schema(description = "消息主题")
    private MsgTopic topic;

    @Schema(description = "消息优先级")
    private MsgPriority priority;

    @Schema(description = "创建时间起点")
    private OffsetDateTime startTime;

    @Schema(description = "创建时间终点")
    private OffsetDateTime endTime;
}
