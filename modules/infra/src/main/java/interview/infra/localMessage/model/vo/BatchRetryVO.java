package interview.infra.localMessage.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote 批量重试结果
 * @since 2026/07/18
 */
@Schema(description = "批量重试结果")
public record BatchRetryVO(
        @Schema(description = "成功数量")
        int successCount,
        @Schema(description = "失败数量")
        int failCount,
        @Schema(description = "重试结果明细") List<RetryItemVO> results
) {
}
