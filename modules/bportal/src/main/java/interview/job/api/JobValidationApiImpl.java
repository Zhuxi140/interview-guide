package interview.job.api;

import interview.api.aicore.dto.JobApplicationSnapshotDTO;
import interview.api.bportal.JobValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.job.model.entity.Job;
import interview.job.model.enums.JobStatus;
import interview.job.service.JobService;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.service.JobApplicationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobValidationApiImpl implements JobValidationApi {

    private final JobService jobService;
    private final JobApplicationsService jobApplicationsService;

    @Override
    public boolean hasActiveJobs(Long enterpriseId) {
        return jobService.lambdaQuery()
                .eq(interview.job.model.entity.Job::getEnterpriseId, enterpriseId)
                .eq(interview.job.model.entity.Job::getStatus, JobStatus.OPEN)
                .exists();
    }

    @Override
    public JobApplicationSnapshotDTO requirePassedApplication(Long applicationId, Long enterpriseId) {

        JobApplications jobApplications = jobApplicationsService.lambdaQuery()
                .select(JobApplications::getEnterpriseId, JobApplications::getJobId,
                        JobApplications::getCandidateId, JobApplications::getStatus)
                .eq(JobApplications::getId, applicationId)
                .one();

        if (jobApplications == null){
            throw new BusinessException(ErrorCode.JOB_APPLICATION_NOT_FOUND);
        }

        Long enterpriseId1 = jobApplications.getEnterpriseId();
        if (!enterpriseId.equals(enterpriseId1)){
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }

        if (!jobApplications.getStatus().equals(JobApplicationStatus.PASSED)){
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }

        return new JobApplicationSnapshotDTO(enterpriseId1, jobApplications.getJobId(), jobApplications.getCandidateId());
    }

    @Override
    public JobApplicationSnapshotDTO requirePassedApplicationWithInfo(Long applicationId, Long enterpriseId) {
        JobApplications jobApplications = jobApplicationsService.lambdaQuery()
                .select(JobApplications::getEnterpriseId, JobApplications::getJobId,
                        JobApplications::getCandidateId, JobApplications::getStatus)
                .eq(JobApplications::getId, applicationId)
                .one();

        if (jobApplications == null){
            throw new BusinessException(ErrorCode.JOB_APPLICATION_NOT_FOUND);
        }

        Long enterpriseId1 = jobApplications.getEnterpriseId();
        if (!enterpriseId.equals(enterpriseId1)){
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }

        // 可发起录用流程的状态：PASSED/INTERVIEWING/OFFERED，已淘汰或已录用不允许再创建 Offer。
        JobApplicationStatus status = jobApplications.getStatus();
        if (status != JobApplicationStatus.PASSED
                && status != JobApplicationStatus.INTERVIEWING
                && status != JobApplicationStatus.OFFERED) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }
        return new JobApplicationSnapshotDTO(jobApplications.getEnterpriseId(), jobApplications.getJobId(), jobApplications.getCandidateId());
    }

    @Override
    public JobApplicationSnapshotDTO requireEligibleForSchedule(Long applicationId, Long enterpriseId) {
        JobApplications jobApplications = jobApplicationsService.lambdaQuery()
                .select(JobApplications::getEnterpriseId, JobApplications::getJobId,
                        JobApplications::getCandidateId, JobApplications::getStatus)
                .eq(JobApplications::getId, applicationId)
                .one();

        if (jobApplications == null){
            throw new BusinessException(ErrorCode.JOB_APPLICATION_NOT_FOUND);
        }

        Long enterpriseId1 = jobApplications.getEnterpriseId();
        if (!enterpriseId.equals(enterpriseId1)){
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }

        JobApplicationStatus status = jobApplications.getStatus();
        if (status != JobApplicationStatus.PASSED && status != JobApplicationStatus.INTERVIEWING) {
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }

        return new JobApplicationSnapshotDTO(enterpriseId1, jobApplications.getJobId(), jobApplications.getCandidateId());
    }

    @Override
    public void markInterviewing(Long applicationId, Long enterpriseId) {
        jobApplicationsService.markInterviewing(enterpriseId, applicationId, AuthContext.getUserIdOrNull() == null ? 0L : AuthContext.getUserIdOrNull());
    }

    @Override
    public void markRejectedByInterview(Long applicationId, Long enterpriseId, String reason) {
        Long operator = AuthContext.getUserIdOrNull();
        jobApplicationsService.markRejectedByInterview(enterpriseId, applicationId, operator == null ? 0L : operator, reason);
    }

    @Override
    public void markOffered(Long applicationId, Long enterpriseId) {
        Long operator = AuthContext.getUserIdOrNull();
        jobApplicationsService.markOffered(enterpriseId, applicationId, operator == null ? 0L : operator);
    }

    @Override
    public void markHired(Long applicationId, Long enterpriseId) {
        Long operator = AuthContext.getUserIdOrNull();
        jobApplicationsService.markHired(enterpriseId, applicationId, operator == null ? 0L : operator);
    }
}
