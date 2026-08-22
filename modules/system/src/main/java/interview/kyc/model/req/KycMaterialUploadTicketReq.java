package interview.kyc.model.req;

import interview.kyc.model.enums.KycMaterialType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 获取本人 KYC 材料短期上传凭证请求。
 */
@Data
@Schema(description = "获取本人 KYC 材料短期上传凭证请求")
public class KycMaterialUploadTicketReq {

    @NotNull(message = "材料类型不能为空")
    @Schema(description = "KYC 材料类型", example = "ID_CARD_FRONT")
    private KycMaterialType materialType;

    @NotBlank(message = "文件名不能为空")
    @Size(max = 256, message = "文件名不能超过 256 个字符")
    @Schema(description = "原始文件名（仅用于生成对象键，服务端不信任其扩展名）",
            example = "id-card-front.jpg")
    private String fileName;

    @NotBlank(message = "文件 MIME 类型不能为空")
    @Size(max = 64, message = "MIME 类型不能超过 64 个字符")
    @Schema(description = "客户端申报的 MIME 类型（服务端上传后会重新检测）",
            example = "image/jpeg")
    private String contentType;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于 0")
    @Max(value = 10 * 1024 * 1024, message = "文件大小不能超过 10MB")
    @Schema(description = "文件大小（字节）", example = "1048576")
    private Long fileSize;

    @NotBlank(message = "文件 SHA-256 摘要不能为空")
    @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "SHA-256 摘要必须为 64 位十六进制字符")
    @Schema(description = "文件 SHA-256 摘要（用于上传完整性校验）",
            example = "3f2a9d8c7b6e5f4a3d2c1b0a99887766554433221100ffeeddccbbaa99887766")
    private String sha256;
}
