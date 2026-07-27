package interview.voiceinterview.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 语音消息查询请求
 * @since 2026/7/26 14:05
 */
@Data
@Schema(description = "语音消息查询请求")
public class VoiceMessageQueryReq {

    @Schema(description = "起始事件序号（不含），默认 0", example = "0")
    private Long afterSequence = 0L;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数，默认 50，最大 100", example = "50")
    private Integer size = 50;
}
