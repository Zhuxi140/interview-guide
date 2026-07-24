package interview.textinterview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.textinterview.model.entity.InterviewReport;
import interview.textinterview.model.vo.*;

public interface InterviewReportService extends IService<InterviewReport> {

    /**
     * B端查询指定排期报告
     * @param enterpriseId 企业ID
     * @param scheduleId 排期ID
     * @return 报告
     */
    InterviewReportVO getReportBySchedule(Long enterpriseId, Long scheduleId);

    /**
     * B端查询企业面试报告列表（分页）
     * @param enterpriseId 企业ID
     * @param page 页码
     * @param size 每页条数
     * @param generationStatus 生成状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 报告分页
     */
    IPage<InterviewReportListItemVO> pageReports(Long enterpriseId, Integer page, Integer size,
                                                  String generationStatus, String startTime, String endTime);

    /**
     * C端查询我的面评报告列表（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页条数
     * @param generationStatus 生成状态
     * @return 报告分页
     */
    IPage<InterviewReportCandidateListItemVO> pageCandidateReports(Long userId, Integer page, Integer size,
                                                                    String generationStatus);

    /**
     * C端查询本人排期报告
     * @param userId 用户ID
     * @param scheduleId 排期ID
     * @return 报告
     */
    InterviewReportVO getCandidateReport(Long userId, Long scheduleId);

    /**
     * C端获取本人报告PDF短期下载地址
     * @param userId 用户ID
     * @param scheduleId 排期ID
     * @return 下载地址
     */
    InterviewReportDownloadVO getDownloadUrl(Long userId, Long scheduleId);
}
