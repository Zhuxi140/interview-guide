package interview.interviewcfg.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.interviewcfg.mapper.WorkflowTransitionLogMapper;
import interview.interviewcfg.model.entity.WorkflowTransitionLog;
import interview.interviewcfg.model.vo.WorkflowLogListItemVO;
import interview.interviewcfg.service.WorkflowTransitionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowTransitionLogServiceImpl
        extends ServiceImpl<WorkflowTransitionLogMapper, WorkflowTransitionLog>
        implements WorkflowTransitionLogService {

    @Override
    public IPage<WorkflowLogListItemVO> pageLogs(Long enterpriseId, Integer page, Integer size, Long scheduleId) {
        // 纯CRUD
        // TODO ① 校验当前用户对 enterpriseId 的访问范围，并限制 page/size，避免管理查询无限放大。
        // TODO ② 日志表没有 enterpriseId，必须通过 interview_schedule 关联并以 schedule.enterpriseId 作为租户过滤条件。
        // TODO ③ scheduleId 非空时同时校验该排期属于当前企业，禁止直接按外部 ID 跨企业读取日志。
        // TODO ④ 按 createdAt、id 倒序分页查询 fromStatus、toStatus、operatorUserId、transitionReason，避免 N+1 查询。
        // TODO ⑤ 映射 WorkflowLogListItemVO，并返回 current、size、total、pages、records 完整分页元数据。
        return null;
    }
}
