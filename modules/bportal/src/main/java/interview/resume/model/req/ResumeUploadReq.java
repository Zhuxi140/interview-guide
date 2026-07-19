package interview.resume.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 简历上传请求（文件 + 元数据，multipart/form-data）
 * <p>实际文件由 MultipartFile 接收，此 Req 仅用于附带可选元数据。</p>
 */
@Data
@Schema(description = "简历上传请求（附带的元数据）")
public class ResumeUploadReq {
    @Schema(description = "文件名")
    private String fileName;
}
