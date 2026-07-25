package interview.interviewcfg.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
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

    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    public IPage<WorkflowLogListItemVO> pageLogs(Long enterpriseId, Integer page,
                                                  Integer size, Long applicationId) {
        // 纯 CRUD
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, userId);
        if (page == null || page < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PAGE_PARAM_INVALID);
        }

        // 日志表自带租户隔离键，查询始终同时限定企业和可选投递。
        IPage<WorkflowTransitionLog> logPage = lambdaQuery()
                .eq(WorkflowTransitionLog::getEnterpriseId, enterpriseId)
                .eq(applicationId != null,
                        WorkflowTransitionLog::getApplicationId, applicationId)
                .orderByDesc(WorkflowTransitionLog::getCreatedAt)
                .orderByDesc(WorkflowTransitionLog::getId)
                .page(new Page<>(page, size));

        // 简单字段投影为对外 VO，不暴露内部 traceId。
        return logPage.convert(log -> new WorkflowLogListItemVO(
                log.getId(),
                log.getApplicationId(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getOperatorUserId(),
                log.getTransitionReason(),
                log.getCreatedAt()
        ));
    }
}
