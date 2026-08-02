package interview.matching.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.CandidateDimensionCode;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.job.model.entity.Job;
import interview.job.service.JobService;
import interview.matching.mapper.JobScreeningConfigMapper;
import interview.matching.model.entity.JobScreeningConfig;
import interview.matching.model.req.JobScreeningConfigUpdateReq;
import interview.matching.model.vo.JobScreeningConfigVO;
import interview.matching.service.JobScreeningConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * 校验岗位归属并使用 CAS 维护 AI 初筛配置。
 */
@Service
@RequiredArgsConstructor
public class JobScreeningConfigServiceImpl
        extends ServiceImpl<JobScreeningConfigMapper, JobScreeningConfig>
        implements JobScreeningConfigService {

    private final EnterpriseValidationApi enterpriseValidationApi;
    private final JobService jobService;
    private final ObjectMapper objectMapper;

    @Override
    public JobScreeningConfigVO getConfig(Long enterpriseId, Long jobId) {
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        validateJob(enterpriseId, jobId);
        JobScreeningConfig config = findConfig(enterpriseId, jobId);
        if (config == null) {
            throw new BusinessException(ErrorCode.JOB_SCREENING_CONFIG_NOT_FOUND);
        }
        return toVO(config);
    }

    @Override
    @Transactional
    public JobScreeningConfigVO saveConfig(
            Long enterpriseId, Long jobId, JobScreeningConfigUpdateReq req) {
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, userId);
        validateJob(enterpriseId, jobId);
        OffsetDateTime now = OffsetDateTime.now();
        String thresholdsJson = writeJson(req.getDimensionThresholds());
        JobScreeningConfig current = findConfig(enterpriseId, jobId);

        if (current == null) {
            if (req.getExpectedVersion() != 0) {
                throw new BusinessException(
                        ErrorCode.JOB_SCREENING_CONFIG_VERSION_CONFLICT);
            }
            JobScreeningConfig created = JobScreeningConfig.builder()
                    .jobId(jobId)
                    .enterpriseId(enterpriseId)
                    .enabled(req.getEnabled())
                    .overallThreshold(req.getOverallThreshold())
                    .dimensionThresholds(thresholdsJson)
                    .version(0)
                    .createdBy(userId)
                    .updatedBy(userId)
                    .traceId(TraceUtil.getTraceId())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            try {
                baseMapper.insert(created);
            } catch (DuplicateKeyException e) {
                throw new BusinessException(
                        ErrorCode.JOB_SCREENING_CONFIG_VERSION_CONFLICT);
            }
            return toVO(created);
        }

        JobScreeningConfig patch = JobScreeningConfig.builder()
                .jobId(jobId)
                .enterpriseId(enterpriseId)
                .enabled(req.getEnabled())
                .overallThreshold(req.getOverallThreshold())
                .dimensionThresholds(thresholdsJson)
                .updatedBy(userId)
                .traceId(TraceUtil.getTraceId())
                .build();
        int updated = baseMapper.updateByExpectedVersion(
                patch, req.getExpectedVersion(), now);
        if (updated != 1) {
            throw new BusinessException(
                    ErrorCode.JOB_SCREENING_CONFIG_VERSION_CONFLICT);
        }
        patch.setVersion(req.getExpectedVersion() + 1);
        patch.setUpdatedAt(now);
        return toVO(patch);
    }

    private void validateJob(Long enterpriseId, Long jobId) {
        boolean exists = jobService.lambdaQuery()
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
    }

    private JobScreeningConfig findConfig(Long enterpriseId, Long jobId) {
        return lambdaQuery()
                .eq(JobScreeningConfig::getJobId, jobId)
                .eq(JobScreeningConfig::getEnterpriseId, enterpriseId)
                .one();
    }

    private JobScreeningConfigVO toVO(JobScreeningConfig config) {
        return new JobScreeningConfigVO(
                config.getJobId(), config.getEnterpriseId(), config.getEnabled(),
                config.getOverallThreshold(), readThresholds(config.getDimensionThresholds()),
                config.getVersion(), config.getUpdatedAt());
    }

    private String writeJson(Map<CandidateDimensionCode, Integer> thresholds) {
        try {
            return objectMapper.writeValueAsString(thresholds);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<CandidateDimensionCode, Integer> readThresholds(String json) {
        try {
            Map<String, Object> raw = objectMapper.readValue(json, Map.class);
            Map<CandidateDimensionCode, Integer> result =
                    new EnumMap<>(CandidateDimensionCode.class);
            raw.forEach((key, value) -> result.put(
                    CandidateDimensionCode.valueOf(key), ((Number) value).intValue()));
            return result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR);
        }
    }
}
