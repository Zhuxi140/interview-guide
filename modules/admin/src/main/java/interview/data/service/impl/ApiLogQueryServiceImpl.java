package interview.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.UserApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.mapper.SysApiLogArchiveMapper;
import interview.data.mapper.SysApiLogMapper;
import interview.data.model.entity.SysApiLog;
import interview.data.model.entity.SysApiLogArchive;
import interview.data.model.vo.ApiLogListItemVO;
import interview.data.service.ApiLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * API 访问日志查询服务实现。
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class ApiLogQueryServiceImpl extends ServiceImpl<SysApiLogMapper, SysApiLog>
        implements ApiLogQueryService {

    private final SysApiLogArchiveMapper sysApiLogArchiveMapper;
    private final UserApi userApi;

    @Override
    public IPage<ApiLogListItemVO> pageApiLogs(Integer page, Integer size, Long userId,
                                               String apiPath, String method, Integer status,
                                               String startTime, String endTime) {
        // 校验分页与时间范围参数。
        validatePage(page, size);
        OffsetDateTime start = parseTime(startTime);
        OffsetDateTime end = parseTime(endTime);
        validateTimeRange(start, end);

        // 单表 LambdaQuery：热表按生成时间倒序稳定分页。
        IPage<SysApiLog> logPage = baseMapper.selectPage(new Page<>(page, size),
                buildLogWrapper(userId, apiPath, method, status, start, end));

        // 跨模块批量补齐用户名，避免逐条查询。
        Map<Long, String> usernameMap = getUsernameMap(
                logPage.getRecords().stream().map(SysApiLog::getUserId).toList());
        Page<ApiLogListItemVO> voPage =
                new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        voPage.setRecords(logPage.getRecords().stream()
                .map(log -> toListItemVO(log.getId(), log.getTraceId(), log.getUserId(),
                        usernameMap.get(log.getUserId()), log.getApiUrl(),
                        log.getRequestMethod(), log.getClientIp(), log.getExecutionTime(),
                        log.getResponseStatus(), log.getErrorMsg(), log.getLlmInputTokens(),
                        log.getLlmOutputTokens(), log.getLlmModel(), log.getCreatedAt()))
                .toList());
        return voPage;
    }

    @Override
    public ApiLogListItemVO getApiLogDetail(Long id) {
        // 查询单条日志并校验存在。
        SysApiLog log = getById(id);
        if (log == null) {
            // TODO: ErrorCode 缺少 API_LOG_NOT_FOUND，暂以参数错误语义返回。
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "API 日志不存在");
        }
        // 单条详情复用用户名批量补齐逻辑。
        Map<Long, String> usernameMap =
                getUsernameMap(log.getUserId() == null ? List.of() : List.of(log.getUserId()));
        return toListItemVO(log.getId(), log.getTraceId(), log.getUserId(),
                usernameMap.get(log.getUserId()), log.getApiUrl(), log.getRequestMethod(),
                log.getClientIp(), log.getExecutionTime(), log.getResponseStatus(),
                log.getErrorMsg(), log.getLlmInputTokens(), log.getLlmOutputTokens(),
                log.getLlmModel(), log.getCreatedAt());
    }

    @Override
    public IPage<ApiLogListItemVO> pageApiLogArchives(Integer page, Integer size, Long userId,
                                                      String apiPath, String method,
                                                      Integer status, String startTime,
                                                      String endTime) {
        // 校验分页与时间范围参数。
        validatePage(page, size);
        OffsetDateTime start = parseTime(startTime);
        OffsetDateTime end = parseTime(endTime);
        validateTimeRange(start, end);

        // 单表 LambdaQuery：冷表结构与热表一致，按生成时间倒序稳定分页。
        LambdaQueryWrapper<SysApiLogArchive> wrapper = new LambdaQueryWrapper<SysApiLogArchive>()
                .eq(userId != null, SysApiLogArchive::getUserId, userId)
                .like(isNotBlank(apiPath), SysApiLogArchive::getApiUrl, apiPath)
                .eq(method != null && !method.isBlank(),
                        SysApiLogArchive::getRequestMethod, method)
                .eq(status != null, SysApiLogArchive::getResponseStatus, status)
                .ge(start != null, SysApiLogArchive::getCreatedAt, start)
                .le(end != null, SysApiLogArchive::getCreatedAt, end)
                .orderByDesc(SysApiLogArchive::getCreatedAt)
                .orderByDesc(SysApiLogArchive::getId);
        IPage<SysApiLogArchive> archivePage =
                sysApiLogArchiveMapper.selectPage(new Page<>(page, size), wrapper);

        // 跨模块批量补齐用户名。
        Map<Long, String> usernameMap = getUsernameMap(
                archivePage.getRecords().stream().map(SysApiLogArchive::getUserId).toList());
        Page<ApiLogListItemVO> voPage = new Page<>(
                archivePage.getCurrent(), archivePage.getSize(), archivePage.getTotal());
        voPage.setRecords(archivePage.getRecords().stream()
                .map(log -> toListItemVO(log.getId(), log.getTraceId(), log.getUserId(),
                        usernameMap.get(log.getUserId()), log.getApiUrl(),
                        log.getRequestMethod(), log.getClientIp(), log.getExecutionTime(),
                        log.getResponseStatus(), log.getErrorMsg(), log.getLlmInputTokens(),
                        log.getLlmOutputTokens(), log.getLlmModel(), log.getCreatedAt()))
                .toList());
        return voPage;
    }

    /**
     * 构建热表查询条件。
     */
    private LambdaQueryWrapper<SysApiLog> buildLogWrapper(Long userId, String apiPath,
                                                          String method, Integer status,
                                                          OffsetDateTime start,
                                                          OffsetDateTime end) {
        return new LambdaQueryWrapper<SysApiLog>()
                .eq(userId != null, SysApiLog::getUserId, userId)
                .like(isNotBlank(apiPath), SysApiLog::getApiUrl, apiPath)
                .eq(method != null && !method.isBlank(),
                        SysApiLog::getRequestMethod, method)
                .eq(status != null, SysApiLog::getResponseStatus, status)
                .ge(start != null, SysApiLog::getCreatedAt, start)
                .le(end != null, SysApiLog::getCreatedAt, end)
                .orderByDesc(SysApiLog::getCreatedAt)
                .orderByDesc(SysApiLog::getId);
    }

    /**
     * 批量查询用户名映射；空列表直接返回空 Map。
     */
    private Map<Long, String> getUsernameMap(List<Long> userIds) {
        List<Long> distinctIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return userApi.getUserNamesByIds(distinctIds);
    }

    private ApiLogListItemVO toListItemVO(Long id, String traceId, Long userId, String username,
                                          String apiPath, String requestMethod, String clientIp,
                                          Long executionTimeMs, Integer responseStatus,
                                          String errorMessage, Integer llmInputTokens,
                                          Integer llmOutputTokens, String llmModel,
                                          java.time.OffsetDateTime createdAt) {
        return new ApiLogListItemVO(id, traceId, userId, username, apiPath, requestMethod,
                clientIp, executionTimeMs, responseStatus, errorMessage, llmInputTokens,
                llmOutputTokens, llmModel, createdAt);
    }

    /**
     * 判断字符串筛选值非空白。
     */
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
