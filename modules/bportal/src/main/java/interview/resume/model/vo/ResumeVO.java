package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 简历详情响应
 */
@Builder
public record ResumeVO(
        Long id,
        Long enterpriseId,
        Long userId,
        String fileName,
        Long fileSize,
        String fileType,
        String fileHash,
        String storageUrl,
        String resumeText,
        String analyzeStatus,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime uploadedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
