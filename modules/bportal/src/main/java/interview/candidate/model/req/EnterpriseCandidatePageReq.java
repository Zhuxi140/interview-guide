package interview.candidate.model.req;

import interview.candidate.model.enums.EnterpriseCandidateSource;
import interview.candidate.model.enums.EnterpriseCandidateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "企业人才池分页查询参数")
public class EnterpriseCandidatePageReq {
    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "候选人来源")
    private EnterpriseCandidateSource source;

    @Schema(description = "人才池状态")
    private EnterpriseCandidateStatus status;

    @Size(max = 64, message = "关键词长度不能超过 64")
    @Schema(description = "候选人姓名关键词")
    private String keyword;
}
