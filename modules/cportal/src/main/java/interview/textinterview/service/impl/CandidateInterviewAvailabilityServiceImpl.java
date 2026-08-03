package interview.textinterview.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.textinterview.mapper.CandidateInterviewAvailabilityMapper;
import interview.textinterview.model.entity.CandidateInterviewAvailability;
import interview.textinterview.model.req.CandidateInterviewAvailabilityUpdateReq;
import interview.textinterview.model.vo.CandidateInterviewAvailabilityVO;
import interview.textinterview.service.CandidateInterviewAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 候选人可面试时间查询与乐观锁更新实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateInterviewAvailabilityServiceImpl
        extends ServiceImpl<CandidateInterviewAvailabilityMapper, CandidateInterviewAvailability>
        implements CandidateInterviewAvailabilityService {

    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private final ObjectMapper objectMapper;

    @Override
    public CandidateInterviewAvailabilityVO getMyAvailability() {
        Long userId = AuthContext.getRequiredUserId();

        // 未设置时返回可直接提交的版本 0 空配置，GET 不产生数据库写入。
        CandidateInterviewAvailability availability = baseMapper.selectById(userId);
        if (availability == null) {
            return new CandidateInterviewAvailabilityVO(
                    DEFAULT_TIMEZONE, List.of(), 0, null);
        }
        return toVO(availability);
    }

    @Override
    @Transactional
    public CandidateInterviewAvailabilityVO updateMyAvailability(
            CandidateInterviewAvailabilityUpdateReq req) {
        Long userId = AuthContext.getRequiredUserId();

        // 校验时区和时间范围，并按实际时间排序后统一存储。
        List<CandidateInterviewAvailabilityUpdateReq.Range> ranges =
                validateAndSort(req.timezone(), req.ranges());
        String rangesJson = writeRanges(userId, ranges);
        OffsetDateTime now = OffsetDateTime.now();
        CandidateInterviewAvailability current = baseMapper.selectById(userId);

        // 首次设置只接受版本 0，利用主键约束处理并发首次写入。
        if (current == null) {
            if (req.expectedVersion() != 0) {
                throw versionConflict();
            }
            CandidateInterviewAvailability created = CandidateInterviewAvailability.builder()
                    .candidateUserId(userId)
                    .timezone(req.timezone())
                    .rangesJson(rangesJson)
                    .version(0)
                    .updatedBy(userId)
                    .traceId(TraceUtil.getTraceId())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            try {
                baseMapper.insert(created);
            } catch (DuplicateKeyException exception) {
                throw versionConflict();
            }
            return toVO(created, ranges);
        }

        // 已有配置必须按 candidate_user_id + version 原子更新并递增版本。
        CandidateInterviewAvailability update = CandidateInterviewAvailability.builder()
                .candidateUserId(userId)
                .timezone(req.timezone())
                .rangesJson(rangesJson)
                .updatedBy(userId)
                .traceId(TraceUtil.getTraceId())
                .updatedAt(now)
                .build();
        int updated = baseMapper.updateByExpectedVersion(update, req.expectedVersion());
        if (updated != 1) {
            throw versionConflict();
        }
        update.setVersion(req.expectedVersion() + 1);
        return toVO(update, ranges);
    }

    private List<CandidateInterviewAvailabilityUpdateReq.Range> validateAndSort(
            String timezone,
            List<CandidateInterviewAvailabilityUpdateReq.Range> ranges) {
        if (timezone == null
                || !ZoneId.getAvailableZoneIds().contains(timezone)
                || ranges == null) {
            throw invalidAvailability();
        }

        List<CandidateInterviewAvailabilityUpdateReq.Range> sorted =
                new ArrayList<>(ranges);
        if (sorted.stream().anyMatch(range -> range == null
                || range.startTime() == null || range.endTime() == null)) {
            throw invalidAvailability();
        }
        sorted.sort(Comparator.comparing(range -> range.startTime().toInstant()));

        Instant previousEnd = null;
        for (CandidateInterviewAvailabilityUpdateReq.Range range : sorted) {
            Instant start = range.startTime().toInstant();
            Instant end = range.endTime().toInstant();
            if (!start.isBefore(end)
                    || previousEnd != null && start.isBefore(previousEnd)) {
                throw invalidAvailability();
            }
            previousEnd = end;
        }
        return List.copyOf(sorted);
    }

    private String writeRanges(
            Long userId,
            List<CandidateInterviewAvailabilityUpdateReq.Range> ranges) {
        try {
            return objectMapper.writeValueAsString(ranges);
        } catch (Exception exception) {
            log.error("候选人可面试时间序列化失败，userId: {}", userId, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private CandidateInterviewAvailabilityVO toVO(
            CandidateInterviewAvailability availability) {
        try {
            CandidateInterviewAvailabilityVO.Range[] ranges = objectMapper.readValue(
                    availability.getRangesJson(),
                    CandidateInterviewAvailabilityVO.Range[].class);
            return new CandidateInterviewAvailabilityVO(
                    availability.getTimezone(),
                    Arrays.asList(ranges),
                    availability.getVersion(),
                    availability.getUpdatedAt());
        } catch (Exception exception) {
            log.error("候选人可面试时间反序列化失败，userId: {}",
                    availability.getCandidateUserId(), exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private CandidateInterviewAvailabilityVO toVO(
            CandidateInterviewAvailability availability,
            List<CandidateInterviewAvailabilityUpdateReq.Range> ranges) {
        List<CandidateInterviewAvailabilityVO.Range> voRanges = ranges.stream()
                .map(range -> new CandidateInterviewAvailabilityVO.Range(
                        range.startTime(), range.endTime()))
                .toList();
        return new CandidateInterviewAvailabilityVO(
                availability.getTimezone(),
                voRanges,
                availability.getVersion(),
                availability.getUpdatedAt());
    }

    private BusinessException versionConflict() {
        return new BusinessException(
                ErrorCode.CANDIDATE_INTERVIEW_AVAILABILITY_VERSION_CONFLICT);
    }

    private BusinessException invalidAvailability() {
        return new BusinessException(
                ErrorCode.CANDIDATE_INTERVIEW_AVAILABILITY_INVALID);
    }
}
