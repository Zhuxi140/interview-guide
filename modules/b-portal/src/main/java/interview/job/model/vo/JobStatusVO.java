package interview.job.model.vo;

import interview.job.model.enums.JobStatus;

/**
 * @author zhuxi
 * @apiNote 开关岗位响应
 */
public record JobStatusVO(
        Long id,
        String title,
        JobStatus status
) {
}
