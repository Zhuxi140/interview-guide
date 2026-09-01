package interview.interviewcfg.calendar.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.interviewcfg.calendar.mapper.InterviewCalendarSlotMapper;
import interview.interviewcfg.calendar.model.entity.InterviewCalendarSlot;
import interview.interviewcfg.calendar.model.enums.CalendarSlotStatus;
import interview.interviewcfg.calendar.model.vo.CalendarSlotListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 面试官日历空闲时段查询服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class CalendarSlotServiceImpl
        extends ServiceImpl<InterviewCalendarSlotMapper, InterviewCalendarSlot>
        implements CalendarSlotService {

    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    public IPage<CalendarSlotListItemVO> pageSlots(Long enterpriseId, Integer page, Integer size,
                                                   Long interviewerUserId, String startTime,
                                                   String endTime, CalendarSlotStatus status) {
        // 校验当前用户属于该企业，并校验分页与时间范围参数。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        validatePage(page, size);
        OffsetDateTime start = parseTime(startTime);
        OffsetDateTime end = parseTime(endTime);
        validateTimeRange(start, end);

        // 单表 LambdaQuery：企业隔离 + 面试官/时间范围/状态筛选，按开始时间倒序稳定分页。
        IPage<InterviewCalendarSlot> slotPage = lambdaQuery()
                .select(InterviewCalendarSlot::getId, InterviewCalendarSlot::getInterviewerUserId,
                        InterviewCalendarSlot::getSlotStart, InterviewCalendarSlot::getSlotEnd,
                        InterviewCalendarSlot::getIsAllocated, InterviewCalendarSlot::getVersion,
                        InterviewCalendarSlot::getCreatedAt)
                .eq(InterviewCalendarSlot::getEnterpriseId, enterpriseId)
                .eq(interviewerUserId != null,
                        InterviewCalendarSlot::getInterviewerUserId, interviewerUserId)
                .ge(start != null, InterviewCalendarSlot::getSlotStart, start)
                .le(end != null, InterviewCalendarSlot::getSlotEnd, end)
                .eq(status != null, InterviewCalendarSlot::getIsAllocated, status.toAllocated())
                .orderByDesc(InterviewCalendarSlot::getSlotStart)
                .orderByDesc(InterviewCalendarSlot::getId)
                .page(new Page<>(page, size));

        // 组装列表 VO，is_allocated 转换为展示状态。
        List<CalendarSlotListItemVO> records = slotPage.getRecords().stream()
                .map(slot -> new CalendarSlotListItemVO(
                        slot.getId(),
                        slot.getInterviewerUserId(),
                        slot.getSlotStart(),
                        slot.getSlotEnd(),
                        Boolean.TRUE.equals(slot.getIsAllocated())
                                ? CalendarSlotStatus.ALLOCATED
                                : CalendarSlotStatus.AVAILABLE,
                        slot.getVersion(),
                        slot.getCreatedAt()))
                .toList();
        Page<CalendarSlotListItemVO> voPage =
                new Page<>(slotPage.getCurrent(), slotPage.getSize(), slotPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteMySlot(Long enterpriseId, Long slotId, Integer expectedVersion) {
        // 校验当前用户属于该企业，再查询时段并校验归属。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        InterviewCalendarSlot slot = lambdaQuery()
                .select(InterviewCalendarSlot::getId, InterviewCalendarSlot::getEnterpriseId,
                        InterviewCalendarSlot::getInterviewerUserId,
                        InterviewCalendarSlot::getIsAllocated,
                        InterviewCalendarSlot::getVersion)
                .eq(InterviewCalendarSlot::getId, slotId)
                .one();
        if (slot == null || !enterpriseId.equals(slot.getEnterpriseId())) {
            // TODO: ErrorCode 缺少 CALENDAR_SLOT_NOT_FOUND，暂以参数错误语义返回。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "日历时段不存在");
        }
        if (!AuthContext.getRequiredUserId().equals(slot.getInterviewerUserId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "仅允许删除本人创建的时段");
        }
        if (Boolean.TRUE.equals(slot.getIsAllocated())) {
            // 已被排期占用的时段必须先取消排期释放后才能删除。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "已被排期占用的时段不允许删除");
        }

        // 按版本条件逻辑删除：乐观锁保证并发下不覆盖其他修改。
        InterviewCalendarSlot update = new InterviewCalendarSlot();
        update.setId(slotId);
        update.setVersion(expectedVersion);
        update.setUpdatedBy(AuthContext.getRequiredUserId());
        update.setUpdatedAt(OffsetDateTime.now());
        int affected = baseMapper.update(update, Wrappers.<InterviewCalendarSlot>lambdaUpdate()
                .eq(InterviewCalendarSlot::getId, slotId)
                .eq(InterviewCalendarSlot::getEnterpriseId, enterpriseId)
                .set(InterviewCalendarSlot::getIsDeleted, true));
        if (affected == 0) {
            // TODO: ErrorCode 缺少 CALENDAR_SLOT_VERSION_CONFLICT，暂以参数错误语义返回。
            throw new BusinessException(
                    ErrorCode.PARAM_VALID_ERROR, "时段已被修改，请刷新后重试");
        }
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }
    }

    private OffsetDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.TIME_FORMAT_INVALID);
        }
    }

    private void validateTimeRange(OffsetDateTime start, OffsetDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ErrorCode.TIME_RANGE_INVALID);
        }
    }
}
