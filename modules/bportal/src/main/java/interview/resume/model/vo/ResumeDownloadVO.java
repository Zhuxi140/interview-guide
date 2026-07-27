package interview.resume.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 简历短期下载地址。
 */
@Schema(description = "简历短期下载地址")
public record ResumeDownloadVO(
        @Schema(description = "预签名下载地址")
        String downloadUrl,
        @Schema(description = "地址有效秒数", example = "300")
        long expiresInSeconds
) {
}
