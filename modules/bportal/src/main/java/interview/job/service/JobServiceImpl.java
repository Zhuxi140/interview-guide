package interview.job.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.dto.EnterprisePublicProfileDTO;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.job.mapper.JobMapper;
import interview.job.model.entity.Job;
import interview.job.model.enums.JobStatus;
import interview.job.model.req.CandidateJobSearchReq;
import interview.job.model.req.JobCreateReq;
import interview.job.model.req.JobListQuery;
import interview.job.model.req.JobStatusReq;
import interview.job.model.req.JobUpdateReq;
import interview.job.model.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhuxi
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {

    private final EnterpriseValidationApi enterpriseValidationApi;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobCreateVO createJob(Long enterpriseId, JobCreateReq req) {
        if (req.getMinSalary() != null && req.getMaxSalary() != null
                && req.getMinSalary().compareTo(req.getMaxSalary()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "最高薪资不能低于最低薪资");
        }
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, userId);
        OffsetDateTime now = OffsetDateTime.now();
        JobStatus status = req.getStatus();
        if (status == JobStatus.CLOSED) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "创建岗位仅允许 DRAFT 或 OPEN");
        }
        JobStatus initialStatus = status != null ? status : JobStatus.DRAFT;
        Job job = Job.builder()
                .enterpriseId(enterpriseId)
                .userId(userId)
                .title(req.getTitle())
                .jdContent(req.getJdContent())
                .department(req.getDepartment())
                .location(req.getLocation())
                .status(initialStatus)
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .experienceReq(req.getExperienceReq())
                .educationReq(req.getEducationReq())
                .skillsJson(writeSkills(req.getSkills()))
                .version(0)
                .createdAt(now)
                .build();

        baseMapper.insert(job);
        return JobCreateVO.builder()
                .id(job.getId())
                .title(req.getTitle())
                .status(initialStatus)
                .version(0)
                .createdAt(now)
                .build();
    }

    @Override
    public IPage<JobListItemVO> pageJobs(Long enterpriseId, JobListQuery query) {
        JobStatus status = query.getStatus();
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
                .select(Job::getId, Job::getTitle,
                        Job::getDepartment, Job::getLocation,
                        Job::getStatus, Job::getVersion, Job::getCreatedAt)
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(status != null, Job::getStatus, status)
                .and(StrUtil.isNotBlank(query.getKeyword()),
                        w -> w.like(Job::getTitle, query.getKeyword())
                                .or()
                                .like(Job::getDepartment, query.getKeyword())
                                .or()
                                .like(Job::getLocation, query.getKeyword())
                )
                .orderByAsc("asc".equalsIgnoreCase(query.getOrder()), Job::getCreatedAt)
                .orderByDesc(!"asc".equalsIgnoreCase(query.getOrder()), Job::getCreatedAt);
        Page<Job> jobPage = baseMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Page<JobListItemVO> voPage = new Page<>(jobPage.getCurrent(), jobPage.getSize(), jobPage.getTotal());
        List<Job> records = jobPage.getRecords();
        List<JobListItemVO> vos = records.stream()
                .map(raw ->
                        JobListItemVO.builder()
                                .id(raw.getId())
                                .title(raw.getTitle())
                                .department(raw.getDepartment())
                                .location(raw.getLocation())
                                .status(raw.getStatus())
                                .version(raw.getVersion())
                                .createdAt(raw.getCreatedAt())
                                .candidateCount(0)
                                .build()
                ).toList();
        voPage.setRecords(vos);

        return voPage;
    }

    @Override
    public JobDetailVO getJobDetail(Long enterpriseId, Long jobId) {
        Job job = lambdaQuery()
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(Job::getId, jobId)
                .one();
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        return JobDetailVO.builder()
                .id(job.getId())
                .createdBy(job.getUserId())
                .title(job.getTitle())
                .jdContent(job.getJdContent())
                .department(job.getDepartment())
                .location(job.getLocation())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .experienceReq(job.getExperienceReq())
                .educationReq(job.getEducationReq())
                .skills(readSkills(job.getSkillsJson()))
                .status(job.getStatus())
                .version(job.getVersion())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    @Override
    public IPage<CandidateJobListItemVO> pageCandidateJobs(CandidateJobSearchReq req) {
        // 通过 system 对外 API 获取允许公开展示的正常企业，避免跨模块直接读取企业表。
        List<EnterprisePublicProfileDTO> enterprises =
                enterpriseValidationApi.listPublicEnterprises(req.getIndustry());
        if (enterprises.isEmpty()) {
            return new Page<>(req.getPage(), req.getSize(), 0);
        }
        Map<Long, EnterprisePublicProfileDTO> enterpriseMap = enterprises.stream()
                .collect(Collectors.toMap(EnterprisePublicProfileDTO::id, Function.identity()));
        List<Long> companyMatchedIds = findCompanyMatchedIds(enterprises, req.getKeyword());

        // 只查询开放、未删除且所属企业可用的岗位，并组合 C 端筛选条件。
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, JobStatus.OPEN)
                .in(Job::getEnterpriseId, enterpriseMap.keySet())
                .like(StrUtil.isNotBlank(req.getCity()), Job::getLocation, req.getCity())
                .eq(req.getExperienceReq() != null, Job::getExperienceReq, req.getExperienceReq())
                .eq(req.getEducationReq() != null, Job::getEducationReq, req.getEducationReq())
                .and(StrUtil.isNotBlank(req.getKeyword()),
                        query -> {
                            query.like(Job::getTitle, req.getKeyword())
                                    .or()
                                    .like(Job::getDepartment, req.getKeyword())
                                    .or()
                                    .like(Job::getJdContent, req.getKeyword());
                            if (!companyMatchedIds.isEmpty()) {
                                query.or().in(Job::getEnterpriseId, companyMatchedIds);
                            }
                        })
                .orderByAsc("asc".equalsIgnoreCase(req.getOrder()), Job::getCreatedAt)
                .orderByDesc(!"asc".equalsIgnoreCase(req.getOrder()), Job::getCreatedAt)
                .orderByAsc("asc".equalsIgnoreCase(req.getOrder()), Job::getId)
                .orderByDesc(!"asc".equalsIgnoreCase(req.getOrder()), Job::getId);

        Page<Job> jobPage = baseMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()),
                wrapper
        );
        Page<CandidateJobListItemVO> result =
                new Page<>(jobPage.getCurrent(), jobPage.getSize(), jobPage.getTotal());
        result.setRecords(jobPage.getRecords().stream()
                .map(job -> toCandidateJobListItem(job, enterpriseMap.get(job.getEnterpriseId())))
                .toList());
        return result;
    }

    @Override
    public CandidateJobDetailVO getCandidateJobDetail(Long jobId) {
        // 岗位本身必须处于开放状态，逻辑删除条件由 MyBatis-Plus 自动附加。
        Job job = baseMapper.selectOne(new LambdaQueryWrapper<Job>()
                .eq(Job::getId, jobId)
                .eq(Job::getStatus, JobStatus.OPEN));
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }

        // 所属企业必须仍为正常状态，否则岗位不能继续对 C 端展示。
        EnterprisePublicProfileDTO enterprise =
                enterpriseValidationApi.getPublicEnterprise(job.getEnterpriseId());
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        return new CandidateJobDetailVO(
                job.getId(),
                job.getEnterpriseId(),
                getEnterpriseDisplayName(enterprise),
                enterprise.industry(),
                enterprise.scale(),
                enterprise.logoUrl(),
                job.getTitle(),
                job.getJdContent(),
                job.getDepartment(),
                job.getLocation(),
                job.getMinSalary(),
                job.getMaxSalary(),
                job.getExperienceReq(),
                job.getEducationReq(),
                readSkills(job.getSkillsJson()),
                job.getCreatedAt()
        );
    }

    private List<Long> findCompanyMatchedIds(
            List<EnterprisePublicProfileDTO> enterprises,
            String keyword
    ) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        return enterprises.stream()
                .filter(enterprise -> StrUtil.containsIgnoreCase(enterprise.name(), keyword)
                        || StrUtil.containsIgnoreCase(enterprise.shortName(), keyword))
                .map(EnterprisePublicProfileDTO::id)
                .toList();
    }

    private CandidateJobListItemVO toCandidateJobListItem(
            Job job,
            EnterprisePublicProfileDTO enterprise
    ) {
        return new CandidateJobListItemVO(
                job.getId(),
                job.getEnterpriseId(),
                getEnterpriseDisplayName(enterprise),
                enterprise.industry(),
                enterprise.logoUrl(),
                job.getTitle(),
                job.getDepartment(),
                job.getLocation(),
                job.getMinSalary(),
                job.getMaxSalary(),
                job.getExperienceReq(),
                job.getEducationReq(),
                readSkills(job.getSkillsJson()),
                job.getCreatedAt()
        );
    }

    private String getEnterpriseDisplayName(EnterprisePublicProfileDTO enterprise) {
        return StrUtil.isNotBlank(enterprise.shortName())
                ? enterprise.shortName()
                : enterprise.name();
    }

    @Override
    @Transactional
    public JobUpdateVO updateJob(Long enterpriseId, Long jobId, JobUpdateReq req) {
        Job existing = lambdaQuery()
                .select(Job::getMinSalary, Job::getMaxSalary, Job::getVersion)
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId)
                .one();
        if (existing == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        BigDecimal mergedMin = req.getMinSalary() != null ? req.getMinSalary() : existing.getMinSalary();
        BigDecimal mergedMax = req.getMaxSalary() != null ? req.getMaxSalary() : existing.getMaxSalary();
        if (mergedMin != null && mergedMax != null && mergedMin.compareTo(mergedMax) > 0) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "最高薪资不能低于最低薪资");
        }
        Long userId = AuthContext.getRequiredUserId();
        OffsetDateTime updatedAt = OffsetDateTime.now();
        Job update = new Job();
        update.setId(jobId);
        update.setEnterpriseId(enterpriseId);
        if (StrUtil.isNotBlank(req.getTitle())) {
            update.setTitle(req.getTitle());
        }
        if (StrUtil.isNotBlank(req.getJdContent())) {
            update.setJdContent(req.getJdContent());
        }
        if (StrUtil.isNotBlank(req.getDepartment())) {
            update.setDepartment(req.getDepartment());
        }
        if (StrUtil.isNotBlank(req.getLocation())) {
            update.setLocation(req.getLocation());
        }
        if (req.getMinSalary() != null) {
            update.setMinSalary(req.getMinSalary());
        }
        if (req.getMaxSalary() != null) {
            update.setMaxSalary(req.getMaxSalary());
        }
        if (req.getExperienceReq() != null) {
            update.setExperienceReq(req.getExperienceReq());
        }
        if (req.getEducationReq() != null) {
            update.setEducationReq(req.getEducationReq());
        }
        if (req.getSkills() != null) {
            update.setSkillsJson(writeSkills(req.getSkills()));
        }
        update.setUpdatedBy(userId);
        update.setUpdatedAt(updatedAt);
        update.setVersion(req.getExpectedVersion() + 1);

        int affected = baseMapper.update(update, Wrappers.<Job>lambdaUpdate()
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(Job::getVersion, req.getExpectedVersion()));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.JOB_VERSION_CONFLICT);
        }
        return new JobUpdateVO(jobId, req.getExpectedVersion() + 1, updatedAt);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobStatusUpdateVO updateJobStatus(Long enterpriseId, Long jobId, JobStatusReq req) {
        if (req.getStatus() == JobStatus.DRAFT) {
            throw new BusinessException(ErrorCode.PARAM_VALID_ERROR, "状态流转仅允许 OPEN 或 CLOSED");
        }
        Long userId = AuthContext.getRequiredUserId();
        OffsetDateTime updatedAt = OffsetDateTime.now();
        int affected = baseMapper.update(null, Wrappers.<Job>lambdaUpdate()
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(Job::getVersion, req.getExpectedVersion())
                .set(Job::getStatus, req.getStatus())
                .set(Job::getVersion, req.getExpectedVersion() + 1)
                .set(Job::getUpdatedBy, userId)
                .set(Job::getTraceId, null)
                .set(Job::getUpdatedAt, updatedAt));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.JOB_VERSION_CONFLICT);
        }
        return new JobStatusUpdateVO(
                jobId, req.getStatus(), req.getExpectedVersion() + 1, updatedAt);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteJob(Long enterpriseId, Long jobId, Integer expectedVersion) {
        Long userId = AuthContext.getRequiredUserId();
        int affected = baseMapper.update(null, Wrappers.<Job>lambdaUpdate()
                .eq(Job::getId, jobId)
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(Job::getVersion, expectedVersion)
                .set(Job::getIsDeleted, true)
                .set(Job::getVersion, expectedVersion + 1)
                .set(Job::getUpdatedBy, userId)
                .set(Job::getTraceId, null)
                .set(Job::getUpdatedAt, OffsetDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.JOB_VERSION_CONFLICT);
        }
    }

    private String writeSkills(List<String> skills) {
        if (skills == null) {
            return null;
        }
        return objectMapper.writeValueAsString(skills);
    }

    private List<String> readSkills(String skillsJson) {
        if (StrUtil.isBlank(skillsJson)) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(skillsJson, String[].class));
        } catch (Exception e) {
            log.error("岗位技能 JSON 解析失败: {}", skillsJson, e);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }
}
