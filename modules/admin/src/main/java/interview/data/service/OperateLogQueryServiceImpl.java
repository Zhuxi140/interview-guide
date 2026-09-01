package interview.data.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.UserApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.mapper.SysOperateLogMapper;
import interview.data.model.entity.SysOperateLog;
import interview.data.model.vo.OperateLogChangeVO;
import interview.data.model.vo.OperateLogDetailVO;
import interview.data.model.vo.OperateLogListItemVO;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 业务操作审计日志查询服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class OperateLogQueryServiceImpl
        extends ServiceImpl<SysOperateLogMapper, SysOperateLog>
        implements OperateLogQueryService {

    private final UserApi userApi;
    private final EnterpriseValidationApi enterpriseValidationApi;

    // TODO: DataAuditInterceptor/TraceFilter 落库接入后产生数据，当前查询接口按空结果正常返回。

    @Override
    public IPage<OperateLogListItemVO> pageOperateLogs(Integer page, Integer size, Long userId,
                                                       String module, String operateType,
                                                       String resourceType, Long resourceId,
                                                       String startTime, String endTime) {
        // 校验分页与时间范围参数。
        validatePage(page, size);
        OffsetDateTime start = parseTime(startTime);
        OffsetDateTime end = parseTime(endTime);
        validateTimeRange(start, end);

        // 单表 LambdaQuery：按操作时间倒序稳定分页。
        IPage<SysOperateLog> logPage = lambdaQuery()
                .select(SysOperateLog::getId, SysOperateLog::getTraceId,
                        SysOperateLog::getUserId, SysOperateLog::getModule,
                        SysOperateLog::getOperateType, SysOperateLog::getTargetTable,
                        SysOperateLog::getTargetId, SysOperateLog::getCreatedAt)
                .eq(userId != null, SysOperateLog::getUserId, userId)
                .eq(isNotBlank(module), SysOperateLog::getModule, module)
                .eq(isNotBlank(operateType), SysOperateLog::getOperateType, operateType)
                .eq(isNotBlank(resourceType), SysOperateLog::getTargetTable, resourceType)
                .eq(resourceId != null, SysOperateLog::getTargetId, resourceId)
                .ge(start != null, SysOperateLog::getCreatedAt, start)
                .le(end != null, SysOperateLog::getCreatedAt, end)
                .orderByDesc(SysOperateLog::getCreatedAt)
                .orderByDesc(SysOperateLog::getId)
                .page(new Page<>(page, size));

        // 跨模块批量补齐用户名后组装列表 VO。
        Map<Long, String> usernameMap = getUsernameMap(logPage.getRecords());
        return toListVOPage(logPage, usernameMap);
    }

    @Override
    public OperateLogDetailVO getOperateLogDetail(Long id) {
        // 查询单条日志并组装字段级脱敏变更。
        SysOperateLog log = requireLog(id);
        return toDetailVO(log, getUsernameMap(List.of(log)));
    }

    @Override
    public List<OperateLogDetailVO> listByTraceId(String traceId) {
        // 参数校验：traceId 必填。
        if (traceId == null || traceId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "traceId 不能为空");
        }
        // 单表查询同链路全部变更，按时间正序还原操作序列。
        List<SysOperateLog> logs = lambdaQuery()
                .eq(SysOperateLog::getTraceId, traceId)
                .orderByAsc(SysOperateLog::getCreatedAt)
                .orderByAsc(SysOperateLog::getId)
                .list();
        Map<Long, String> usernameMap = getUsernameMap(logs);
        return logs.stream()
                .map(log -> toDetailVO(log, usernameMap))
                .toList();
    }

    @Override
    public IPage<OperateLogListItemVO> pageEnterpriseOperateLogs(
            Long enterpriseId, Integer page, Integer size, String module, String operateType,
            String resourceType, Long resourceId, String startTime, String endTime) {
        // 校验当前用户属于该企业，并校验分页与时间范围参数。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        validatePage(page, size);
        OffsetDateTime start = parseTime(startTime);
        OffsetDateTime end = parseTime(endTime);
        validateTimeRange(start, end);

        // 两表 JOIN（XML）：限定企业成员产生的操作记录，强制企业数据域。
        IPage<SysOperateLog> logPage = baseMapper.pageByEnterprise(
                new Page<>(page, size),
                enterpriseId,
                isNotBlank(module) ? module : null,
                isNotBlank(operateType) ? operateType : null,
                isNotBlank(resourceType) ? resourceType : null,
                resourceId,
                start,
                end);

        // 跨模块批量补齐用户名后组装列表 VO。
        Map<Long, String> usernameMap = getUsernameMap(logPage.getRecords());
        return toListVOPage(logPage, usernameMap);
    }

    @Override
    public OperateLogDetailVO getEnterpriseOperateLogDetail(Long enterpriseId, Long id) {
        // 校验当前用户属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        // 查询日志并复用成员校验确认日志操作人属于该企业，实现数据域隔离。
        SysOperateLog log = requireLog(id);
        if (log.getUserId() != null) {
            enterpriseValidationApi.validateEnterpriseMembers(
                    enterpriseId, List.of(log.getUserId()));
        }
        return toDetailVO(log, getUsernameMap(List.of(log)));
    }

    /**
     * 查询日志并统一处理不存在。
     */
    private SysOperateLog requireLog(Long id) {
        SysOperateLog log = getById(id);
        if (log == null) {
            // TODO: ErrorCode 缺少 OPERATE_LOG_NOT_FOUND，暂以参数错误语义返回。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "审计日志不存在");
        }
        return log;
    }

    /**
     * 批量查询用户名映射；空列表直接返回空 Map。
     */
    private Map<Long, String> getUsernameMap(List<SysOperateLog> logs) {
        List<Long> userIds = logs.stream()
                .map(SysOperateLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userApi.getUserNamesByIds(userIds);
    }

    /**
     * 将日志分页转换为列表 VO 分页。
     */
    private IPage<OperateLogListItemVO> toListVOPage(IPage<SysOperateLog> logPage,
                                                     Map<Long, String> usernameMap) {
        Page<OperateLogListItemVO> voPage =
                new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        voPage.setRecords(logPage.getRecords().stream()
                .map(log -> toListItemVO(log, usernameMap))
                .toList());
        return voPage;
    }

    private OperateLogListItemVO toListItemVO(SysOperateLog log,
                                              Map<Long, String> usernameMap) {
        return new OperateLogListItemVO(
                log.getId(),
                log.getTraceId(),
                log.getUserId(),
                usernameMap.get(log.getUserId()),
                log.getModule(),
                log.getOperateType(),
                log.getTargetTable(),
                log.getTargetId(),
                log.getCreatedAt());
    }

    /**
     * 组装详情 VO：基于前后快照 JSON 计算字段级变更（脱敏展示，不暴露物理表名）。
     */
    private OperateLogDetailVO toDetailVO(SysOperateLog log,
                                          Map<Long, String> usernameMap) {
        return new OperateLogDetailVO(
                log.getId(),
                log.getTraceId(),
                log.getUserId(),
                usernameMap.get(log.getUserId()),
                log.getModule(),
                log.getOperateType(),
                log.getTargetTable(),
                log.getTargetId(),
                diffChanges(log.getOldValueJson(), log.getNewValueJson()),
                log.getCreatedAt());
    }

    /**
     * 对比修改前后快照，输出字段级变更；解析失败或无快照时返回空列表。
     */
    private List<OperateLogChangeVO> diffChanges(String oldValueJson, String newValueJson) {
        JSONObject before = parseSnapshot(oldValueJson);
        JSONObject after = parseSnapshot(newValueJson);
        if (before == null && after == null) {
            return List.of();
        }
        // 合并两侧字段名并保持稳定顺序。
        Set<String> fields = new LinkedHashSet<>();
        if (before != null) {
            fields.addAll(before.keySet());
        }
        if (after != null) {
            fields.addAll(after.keySet());
        }
        List<OperateLogChangeVO> changes = new ArrayList<>();
        for (String field : fields) {
            Object beforeValue = before == null ? null : before.get(field);
            Object afterValue = after == null ? null : after.get(field);
            if (!Objects.equals(String.valueOf(beforeValue), String.valueOf(afterValue))) {
                changes.add(new OperateLogChangeVO(field, beforeValue, afterValue));
            }
        }
        return changes;
    }

    /**
     * 解析快照 JSON；空白或非法内容返回 null。
     */
    private JSONObject parseSnapshot(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(snapshotJson);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
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
