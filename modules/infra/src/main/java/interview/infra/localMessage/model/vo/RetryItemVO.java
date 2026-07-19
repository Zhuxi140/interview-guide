package interview.infra.localMessage.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author zhuxi
 * @apiNote 单条重试结果项
 * @since 2026/07/18
 */
@Schema(description = "单条重试结果项")
public record RetryItemVO(
        @Schema(description = "消息ID")
        Long id,
        @Schema(description = "是否成功")
        boolean success,
        @Schema(description = "错误信息")
        String error
) {
}
