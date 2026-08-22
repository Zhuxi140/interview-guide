package interview.interviewcfg.calendar.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.interviewcfg.calendar.model.enums.CalendarSlotStatus;
import interview.interviewcfg.calendar.model.vo.CalendarSlotListItemVO;

/**
 * 面试官日历空闲时段查询服务。
 *
 * @author zhuxi
 */
public interface CalendarSlotService {

    /**
     * 分页查询企业内面试官的日历空闲时段。
     *
     * @param enterpriseId      企业 ID
     * @param page              页码
     * @param size              每页条数
     * @param interviewerUserId 面试官用户 ID 筛选，可为空
     * @param startTime         时段开始下界（ISO-8601 带时区），可为空
     * @param endTime           时段结束上界（ISO-8601 带时区），可为空
     * @param status            时段状态筛选，可为空
     * @return 时段分页结果
     */
    IPage<CalendarSlotListItemVO> pageSlots(Long enterpriseId, Integer page, Integer size,
                                            Long interviewerUserId, String startTime,
                                            String endTime, CalendarSlotStatus status);

    /**
     * 删除本人尚未被排期占用的空闲时段（按 If-Match 版本条件删除）。
     *
     * @param enterpriseId    企业 ID
     * @param slotId          时段 ID
     * @param expectedVersion 客户端持有的时段版本号
     */
    void deleteMySlot(Long enterpriseId, Long slotId, Integer expectedVersion);
}
