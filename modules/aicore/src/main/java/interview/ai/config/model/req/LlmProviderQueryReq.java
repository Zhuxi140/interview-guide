package interview.ai.config.model.req;

import interview.common.enums.AiModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 大模型提供商配置分页查询请求
 * @since 2026/7/26 14:05
 */
@Data
@Schema(description = "大模型提供商配置分页查询请求")
public class LlmProviderQueryReq {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "路由开关过滤")
    private Boolean enabled;

    @Schema(description = "模型能力类型过滤", example = "CHAT")
    private AiModelType modelType;

    @Schema(description = "关键字搜索（providerId / baseUrl / model）")
    private String keyword;

    @Pattern(
            regexp = "providerId|baseUrl|model|modelType|enabled|createdAt|updatedAt",
            message = "排序字段不合法"
    )
    @Schema(
            description = "排序字段",
            allowableValues = {"providerId", "baseUrl", "model", "modelType", "enabled", "createdAt", "updatedAt"},
            example = "createdAt"
    )
    private String sort = "createdAt";

    @Pattern(regexp = "(?i)asc|desc", message = "排序方向必须为 asc 或 desc")
    @Schema(description = "排序方向", allowableValues = {"asc", "desc"}, example = "desc")
    private String order = "desc";
}
