package interview.candidate.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.cportal.InterviewReportQueryApi;
import interview.api.system.EnterpriseValidationApi;
import interview.candidate.mapper.EnterpriseCandidateMapper;
import interview.candidate.mapper.ResumeImportItemMapper;
import interview.candidate.model.entity.EnterpriseCandidate;
import interview.candidate.model.entity.ResumeImportItem;
import interview.candidate.model.req.EnterpriseCandidatePageReq;
import interview.candidate.model.vo.*;
import interview.code.model.req.EnterpriseCodeSubmissionSearchReq;
import interview.code.model.vo.EnterpriseCodeSubmissionListItemVO;
import interview.code.service.CodeSubmissionQueryService;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.interviewcfg.mapper.InterviewScheduleMapper;
import interview.interviewcfg.model.entity.InterviewSchedule;
import interview.matching.mapper.JobApplicationsMapper;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.enums.JobApplicationStatus;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.mapper.CandidateSkillScoresMapper;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.model.entity.CandidateSkillScores;
import interview.resume.model.vo.CandidateProfileVO;
import interview.resume.model.vo.CandidateSkillScoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnterpriseCandidateServiceImpl
        extends ServiceImpl<EnterpriseCandidateMapper, EnterpriseCandidate>
        implements EnterpriseCandidateService {

    private final EnterpriseValidationApi enterpriseValidationApi;
    private final ResumeImportItemMapper resumeImportItemMapper;
    private final JobApplicationsMapper jobApplicationsMapper;
    private final CandidateProfileMapper candidateProfileMapper;
    private final CandidateSkillScoresMapper candidateSkillScoresMapper;
    private final InterviewScheduleMapper interviewScheduleMapper;
    private final InterviewReportQueryApi interviewReportQueryApi;
    private final CodeSubmissionQueryService codeSubmissionQueryService;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<EnterpriseCandidateListItemVO> pageCandidates(
            Long enterpriseId, EnterpriseCandidatePageReq req) {
        validateEnterprise(enterpriseId);
        IPage<EnterpriseCandidate> candidatePage = lambdaQuery()
                .select(
                        EnterpriseCandidate::getId,
                        EnterpriseCandidate::getLinkedUserId,
                        EnterpriseCandidate::getCandidateName,
                        EnterpriseCandidate::getSource,
                        EnterpriseCandidate::getStatus,
                        EnterpriseCandidate::getUpdatedAt)
                .eq(EnterpriseCandidate::getEnterpriseId, enterpriseId)
                .eq(req.getSource() != null, EnterpriseCandidate::getSource, req.getSource())
                .eq(req.getStatus() != null, EnterpriseCandidate::getStatus, req.getStatus())
                .like(StrUtil.isNotBlank(req.getKeyword()),
                        EnterpriseCandidate::getCandidateName, req.getKeyword())
                .orderByDesc(EnterpriseCandidate::getUpdatedAt)
                .orderByDesc(EnterpriseCandidate::getId)
                .page(new Page<>(req.getPage(), req.getSize()));

        List<Long> linkedUserIds = candidatePage.getRecords().stream()
                .map(EnterpriseCandidate::getLinkedUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, JobApplicationStatus> applicationStatuses = latestApplicationStatuses(
                enterpriseId, linkedUserIds);
        Map<Long, AiTaskStatus> profileStatuses = latestProfileStatuses(linkedUserIds);
        return candidatePage.convert(candidate -> new EnterpriseCandidateListItemVO(
                candidate.getId(), candidate.getCandidateName(), candidate.getSource(),
                candidate.getStatus(), applicationStatuses.get(candidate.getLinkedUserId()),
                profileStatuses.get(candidate.getLinkedUserId()), candidate.getUpdatedAt()));
    }

    @Override
    public EnterpriseCandidateDetailVO getCandidate(Long enterpriseId, Long candidateId) {
        validateEnterprise(enterpriseId);
        EnterpriseCandidate candidate = requireCandidate(enterpriseId, candidateId);
        List<EnterpriseCandidateApplicationVO> applications = listApplications(
                enterpriseId, candidate.getLinkedUserId());
        return new EnterpriseCandidateDetailVO(
                candidate.getId(), candidate.getCandidateName(),
                new CandidateContactMaskedVO(
                        maskPhone(candidate.getPhone()), maskEmail(candidate.getEmail())),
                listImportedResumes(enterpriseId, candidateId),
                latestCompletedProfile(candidate.getLinkedUserId()),
                applications);
    }

    @Override
    public EnterpriseCandidateOverviewVO getOverview(Long enterpriseId, Long candidateId) {
        validateEnterprise(enterpriseId);
        EnterpriseCandidate candidate = requireCandidate(enterpriseId, candidateId);
        List<EnterpriseCandidateApplicationVO> applications = listApplications(
                enterpriseId, candidate.getLinkedUserId());
        List<Long> applicationIds = applications.stream()
                .map(EnterpriseCandidateApplicationVO::applicationId)
                .toList();
        List<InterviewSchedule> schedules = applicationIds.isEmpty()
                ? List.of()
                : interviewScheduleMapper.selectList(
                        Wrappers.<InterviewSchedule>lambdaQuery()
                                .select(
                                        InterviewSchedule::getId,
                                        InterviewSchedule::getApplicationId,
                                        InterviewSchedule::getRoundNo,
                                        InterviewSchedule::getPhaseCode,
                                        InterviewSchedule::getStatus,
                                        InterviewSchedule::getInterviewTime)
                                .eq(InterviewSchedule::getEnterpriseId, enterpriseId)
                                .in(InterviewSchedule::getApplicationId, applicationIds)
                                .orderByAsc(InterviewSchedule::getApplicationId)
                                .orderByAsc(InterviewSchedule::getRoundNo));
        List<Long> scheduleIds = schedules.stream().map(InterviewSchedule::getId).toList();
        List<EnterpriseCandidateReportVO> reports = interviewReportQueryApi
                .listReports(enterpriseId, scheduleIds)
                .stream()
                .map(report -> new EnterpriseCandidateReportVO(
                        report.reportId(), report.scheduleId(), report.status(),
                        report.overallAiScore(), report.completedAt()))
                .toList();

        List<EnterpriseCodeSubmissionListItemVO> submissions = List.of();
        if (candidate.getLinkedUserId() != null && !applications.isEmpty()) {
            EnterpriseCodeSubmissionSearchReq searchReq = new EnterpriseCodeSubmissionSearchReq();
            searchReq.setPage(1);
            searchReq.setSize(100);
            submissions = codeSubmissionQueryService.pageEnterpriseCandidateSubmissions(
                    enterpriseId, candidate.getLinkedUserId(), searchReq).getRecords();
        }
        return new EnterpriseCandidateOverviewVO(
                candidateId,
                latestCompletedProfile(candidate.getLinkedUserId()),
                applications,
                schedules.stream()
                        .map(schedule -> new EnterpriseCandidateInterviewRoundVO(
                                schedule.getId(), schedule.getApplicationId(),
                                schedule.getRoundNo(), schedule.getPhaseCode(),
                                schedule.getStatus(), schedule.getInterviewTime()))
                        .toList(),
                reports,
                submissions);
    }

    private void validateEnterprise(Long enterpriseId) {
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
    }

    private EnterpriseCandidate requireCandidate(Long enterpriseId, Long candidateId) {
        EnterpriseCandidate candidate = lambdaQuery()
                .eq(EnterpriseCandidate::getId, candidateId)
                .eq(EnterpriseCandidate::getEnterpriseId, enterpriseId)
                .one();
        if (candidate == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CANDIDATE_NOT_FOUND);
        }
        return candidate;
    }

    private List<EnterpriseCandidateResumeVO> listImportedResumes(
            Long enterpriseId, Long candidateId) {
        return resumeImportItemMapper.selectList(
                        Wrappers.<ResumeImportItem>lambdaQuery()
                                .select(
                                        ResumeImportItem::getId,
                                        ResumeImportItem::getOriginalFilename,
                                        ResumeImportItem::getStatus,
                                        ResumeImportItem::getErrorMessage)
                                .eq(ResumeImportItem::getEnterpriseId, enterpriseId)
                                .eq(ResumeImportItem::getEnterpriseCandidateId, candidateId)
                                .orderByDesc(ResumeImportItem::getCreatedAt))
                .stream()
                .map(item -> new EnterpriseCandidateResumeVO(
                        item.getId(), item.getOriginalFilename(),
                        item.getStatus(), item.getErrorMessage()))
                .toList();
    }

    private List<EnterpriseCandidateApplicationVO> listApplications(
            Long enterpriseId, Long linkedUserId) {
        if (linkedUserId == null) {
            return List.of();
        }
        return jobApplicationsMapper.selectList(
                        Wrappers.<JobApplications>lambdaQuery()
                                .select(
                                        JobApplications::getId,
                                        JobApplications::getJobId,
                                        JobApplications::getStatus,
                                        JobApplications::getCreatedAt)
                                .eq(JobApplications::getEnterpriseId, enterpriseId)
                                .eq(JobApplications::getCandidateId, linkedUserId)
                                .orderByDesc(JobApplications::getCreatedAt))
                .stream()
                .map(application -> new EnterpriseCandidateApplicationVO(
                        application.getId(), application.getJobId(),
                        application.getStatus(), application.getCreatedAt()))
                .toList();
    }

    private Map<Long, JobApplicationStatus> latestApplicationStatuses(
            Long enterpriseId, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, JobApplicationStatus> statuses = new HashMap<>();
        jobApplicationsMapper.selectList(
                        Wrappers.<JobApplications>lambdaQuery()
                                .select(JobApplications::getCandidateId, JobApplications::getStatus)
                                .eq(JobApplications::getEnterpriseId, enterpriseId)
                                .in(JobApplications::getCandidateId, userIds)
                                .orderByDesc(JobApplications::getCreatedAt))
                .forEach(application -> statuses.putIfAbsent(
                        application.getCandidateId(), application.getStatus()));
        return statuses;
    }

    private Map<Long, AiTaskStatus> latestProfileStatuses(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AiTaskStatus> statuses = new HashMap<>();
        candidateProfileMapper.selectList(
                        Wrappers.<CandidateProfile>lambdaQuery()
                                .select(CandidateProfile::getCandidateId, CandidateProfile::getStatus)
                                .in(CandidateProfile::getCandidateId, userIds)
                                .orderByDesc(CandidateProfile::getCreatedAt))
                .forEach(profile -> statuses.putIfAbsent(
                        profile.getCandidateId(), profile.getStatus()));
        return statuses;
    }

    private CandidateProfileVO latestCompletedProfile(Long linkedUserId) {
        if (linkedUserId == null) {
            return null;
        }
        CandidateProfile profile = candidateProfileMapper.selectOne(
                Wrappers.<CandidateProfile>lambdaQuery()
                        .eq(CandidateProfile::getCandidateId, linkedUserId)
                        .eq(CandidateProfile::getStatus, AiTaskStatus.COMPLETED)
                        .orderByDesc(CandidateProfile::getAnalyzedAt)
                        .last("LIMIT 1"));
        if (profile == null) {
            return null;
        }
        List<CandidateSkillScoreVO> dimensions = candidateSkillScoresMapper.selectList(
                        Wrappers.<CandidateSkillScores>lambdaQuery()
                                .eq(CandidateSkillScores::getCandidateProfileId, profile.getId())
                                .orderByAsc(CandidateSkillScores::getDimensionCode))
                .stream()
                .map(score -> new CandidateSkillScoreVO(
                        score.getDimensionCode(), score.getScore(),
                        score.getAiJustification(), readStringList(score.getEvidenceJson())))
                .toList();
        return new CandidateProfileVO(
                profile.getId(), profile.getResumeId(), profile.getStatus(),
                profile.getProfileSchemaVersion(), readSummary(profile.getSummaryJson()),
                dimensions, profile.getAnalyzedAt());
    }

    private String readSummary(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            Map<?, ?> value = objectMapper.readValue(json, Map.class);
            return value.get("summary") == null ? null : value.get("summary").toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            return (List<String>) (List<?>) objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return StrUtil.isBlank(phone) ? null : "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
}
