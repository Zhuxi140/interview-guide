package interview.api.cportal;

import interview.api.cportal.dto.InterviewReportSummaryDTO;

import java.util.Collection;
import java.util.List;

public interface InterviewReportQueryApi {

    /**
     * 批量查询企业排期对应的面试报告摘要
     * @param enterpriseId 企业 ID
     * @param scheduleIds 排期 ID 集合
     * @return 报告摘要列表
     */
    List<InterviewReportSummaryDTO> listReports(
            Long enterpriseId, Collection<Long> scheduleIds);
}
