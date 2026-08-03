package interview.textinterview.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewSchedulePageDTO;
import interview.common.enums.InterviewScheduleStatus;
import interview.framework.context.AuthContext;
import interview.textinterview.model.vo.InterviewScheduleCandidateListItemVO;
import interview.textinterview.service.CandidateInterviewQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 候选人面试只读查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class CandidateInterviewQueryServiceImpl implements CandidateInterviewQueryService {

    private final InterviewScheduleQueryApi interviewScheduleQueryApi;

    @Override
    public IPage<InterviewScheduleCandidateListItemVO> pageMySchedules(Integer page,
                                                                       Integer size,
                                                                       InterviewScheduleStatus status,
                                                                       String sort,
                                                                       String order) {
        // 纯 CRUD
        // 使用认证上下文中的候选人 ID 调用排期所属模块。
        InterviewSchedulePageDTO schedulePage =
                interviewScheduleQueryApi.pageCandidateSchedules(
                        AuthContext.getRequiredUserId(), page, size, status, sort, order);
        List<InterviewScheduleCandidateListItemVO> records =
                schedulePage.records().stream()
                        .map(schedule -> new InterviewScheduleCandidateListItemVO(
                                schedule.id(),
                                schedule.enterpriseName(),
                                schedule.jobTitle(),
                                schedule.roundNo(),
                                schedule.phaseCode(),
                                schedule.phaseName(),
                                schedule.interviewTime(),
                                schedule.durationMinutes(),
                                schedule.interviewType(),
                                schedule.status(),
                                schedule.version()
                        ))
                        .toList();
        Page<InterviewScheduleCandidateListItemVO> result =
                new Page<>(schedulePage.current(), schedulePage.size(), schedulePage.total());
        result.setRecords(records);
        return result;
    }
}
