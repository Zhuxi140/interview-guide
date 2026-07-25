package interview.api.bportal.dto;

import java.util.List;

/**
 * 面试排期分页 DTO。
 *
 * @param current 当前页
 * @param size 每页条数
 * @param total 总记录数
 * @param pages 总页数
 * @param records 排期记录
 */
public record InterviewSchedulePageDTO(
        long current,
        long size,
        long total,
        long pages,
        List<InterviewScheduleQueryDTO> records
) {
}
