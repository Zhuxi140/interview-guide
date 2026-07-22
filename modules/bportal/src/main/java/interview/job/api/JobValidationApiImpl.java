package interview.job.api;

import interview.api.bportal.JobValidationApi;
import interview.job.model.enums.JobStatus;
import interview.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobValidationApiImpl implements JobValidationApi {

    private final JobService jobService;

    @Override
    public boolean hasActiveJobs(Long enterpriseId) {
        return jobService.lambdaQuery()
                .eq(interview.job.model.entity.Job::getEnterpriseId, enterpriseId)
                .eq(interview.job.model.entity.Job::getStatus, JobStatus.OPEN)
                .exists();
    }
}
