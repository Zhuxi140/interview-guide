package interview.textinterview.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.cportal.InterviewReportQueryApi;
import interview.api.cportal.dto.InterviewReportSummaryDTO;
import interview.textinterview.mapper.InterviewReportMapper;
import interview.textinterview.model.entity.InterviewReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewReportQueryApiImpl implements InterviewReportQueryApi {

    private final InterviewReportMapper interviewReportMapper;

    @Override
    public List<InterviewReportSummaryDTO> listReports(
            Long enterpriseId, Collection<Long> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return List.of();
        }
        return interviewReportMapper.selectList(
                        Wrappers.<InterviewReport>lambdaQuery()
                                .select(
                                        InterviewReport::getId,
                                        InterviewReport::getScheduleId,
                                        InterviewReport::getGenerationStatus,
                                        InterviewReport::getOverallAiScore,
                                        InterviewReport::getCompletedAt)
                                .eq(InterviewReport::getEnterpriseId, enterpriseId)
                                .in(InterviewReport::getScheduleId, scheduleIds)
                                .orderByAsc(InterviewReport::getScheduleId))
                .stream()
                .map(report -> new InterviewReportSummaryDTO(
                        report.getId(), report.getScheduleId(),
                        report.getGenerationStatus(), report.getOverallAiScore(),
                        report.getCompletedAt()))
                .toList();
    }
}
