package interview.voiceinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 语音消息分页响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "语音消息分页响应")
public record VoiceMessagePageVO(
        @Schema(description = "下一页起始序号")
        Long nextSequence,

        @Schema(description = "是否还有更多数据")
        Boolean hasMore,

        @Schema(description = "消息列表")
        List<VoiceMessageVO> records
) {
}
