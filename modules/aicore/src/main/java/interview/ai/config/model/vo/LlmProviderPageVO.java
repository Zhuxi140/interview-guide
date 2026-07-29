package interview.ai.config.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 大模型提供商配置分页响应
 * @since 2026/7/26 14:05
 */
@Builder
@Schema(description = "大模型提供商配置分页响应")
public record LlmProviderPageVO(
        @Schema(description = "当前页码")
        Long current,

        @Schema(description = "每页条数")
        Integer size,

        @Schema(description = "总记录数")
        Long total,

        @Schema(description = "总页数")
        Long pages,

        @Schema(description = "记录列表")
        List<LlmProviderVO> records
) {
}
