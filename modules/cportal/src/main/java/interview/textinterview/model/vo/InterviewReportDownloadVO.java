package interview.textinterview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "报告 PDF 下载地址响应")
public record InterviewReportDownloadVO(
        @Schema(description = "短期预签名下载地址")
        String downloadUrl,

        @Schema(description = "链接有效期（秒）", example = "300")
        Long expiresInSeconds
) {
}
