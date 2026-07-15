package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.resume.model.enums.AnalyzeStatus;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 简历列表项响应
 */
@Builder
public record ResumeListItemVO(
        Long id,
        String fileName,
        String fileType,
        Long fileSize,
        AnalyzeStatus analyzeStatus,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
