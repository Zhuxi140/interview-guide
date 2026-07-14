package interview.resume.model.req;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 简历列表查询参数
 */
@Data
public class ResumeListQuery {

    private String fileName;

    private Long userId;

    private String analyzeStatus;

    @Min(value = 1, message = "页码最小为 1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    private Integer size = 20;
}
