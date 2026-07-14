package interview.resume.model.req;

import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 简历上传请求（文件 + 元数据，multipart/form-data）
 * <p>实际文件由 MultipartFile 接收，此 Req 仅用于附带可选元数据。</p>
 */
@Data
public class ResumeUploadReq {

    private String fileName;
}
