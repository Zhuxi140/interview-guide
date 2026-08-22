package interview.interviewcfg.calendar.model.enums;

/**
 * 日历空闲时段展示状态，对应 is_allocated 标记。
 */
public enum CalendarSlotStatus {

    /**
     * 空闲开放中（is_allocated = FALSE）。
     */
    AVAILABLE,

    /**
     * 已排期锁定（is_allocated = TRUE）。
     */
    ALLOCATED;

    /**
     * 将状态转换为 is_allocated 存储值。
     *
     * @return true 表示已排期锁定
     */
    public boolean toAllocated() {
        return this == ALLOCATED;
    }
}
