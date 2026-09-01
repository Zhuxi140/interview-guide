package interview.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.notification.mapper.NotificationSendRecordMapper;
import interview.notification.model.entity.NotificationSendRecord;
import interview.notification.model.enums.ChannelType;
import interview.notification.model.enums.SendStatus;
import interview.notification.model.vo.NotificationSendRecordListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 全站通知发送记录查询服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class NotificationSendRecordServiceImpl
        extends ServiceImpl<NotificationSendRecordMapper, NotificationSendRecord>
        implements NotificationSendRecordService {

    @Override
    public IPage<NotificationSendRecordListItemVO> pageRecords(Integer page, Integer size,
                                                                String channelType,
                                                                String sendStatus,
                                                                String startTime,
                                                                String endTime) {
        // 校验分页、枚举与时间范围参数。
        validatePage(page, size);
        String channel = normalizeChannel(channelType);
        String status = normalizeStatus(sendStatus);
        OffsetDateTime start = parseTime(startTime);
        OffsetDateTime end = parseTime(endTime);
        validateTimeRange(start, end);

        // 单表 LambdaQuery：复用 sys_notifications 投递流水，按发送时间倒序稳定分页。
        IPage<NotificationSendRecord> recordPage = lambdaQuery()
                .select(NotificationSendRecord::getId, NotificationSendRecord::getUserId,
                        NotificationSendRecord::getNotifyScene,
                        NotificationSendRecord::getChannelType,
                        NotificationSendRecord::getSendStatus,
                        NotificationSendRecord::getFailureReason,
                        NotificationSendRecord::getCreatedAt)
                .eq(channel != null, NotificationSendRecord::getChannelType, channel)
                .eq(status != null, NotificationSendRecord::getSendStatus, status)
                .ge(start != null, NotificationSendRecord::getCreatedAt, start)
                .le(end != null, NotificationSendRecord::getCreatedAt, end)
                .orderByDesc(NotificationSendRecord::getCreatedAt)
                .orderByDesc(NotificationSendRecord::getId)
                .page(new Page<>(page, size));

        // 组装列表 VO。
        List<NotificationSendRecordListItemVO> records = recordPage.getRecords().stream()
                .map(record -> new NotificationSendRecordListItemVO(
                        record.getId(),
                        record.getUserId(),
                        record.getNotifyScene(),
                        record.getChannelType(),
                        record.getSendStatus(),
                        record.getFailureReason(),
                        record.getCreatedAt()))
                .toList();
        Page<NotificationSendRecordListItemVO> voPage = new Page<>(
                recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        voPage.setRecords(records);
        return voPage;
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

    /**
     * 校验并返回合法的渠道类型；空白入参返回 null 表示不过滤。
     */
    private String normalizeChannel(String channelType) {
        if (channelType == null || channelType.isBlank()) {
            return null;
        }
        try {
            return ChannelType.valueOf(channelType).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的渠道类型");
        }
    }

    /**
     * 校验并返回合法的发送状态；空白入参返回 null 表示不过滤。
     */
    private String normalizeStatus(String sendStatus) {
        if (sendStatus == null || sendStatus.isBlank()) {
            return null;
        }
        try {
            return SendStatus.valueOf(sendStatus).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的发送状态");
        }
    }
}
