package interview.matching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.matching.model.bo.JobApplicationBO;
import interview.matching.model.bo.JobApplicationListBO;
import interview.matching.model.bo.MyApplicationListBO;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.vo.JobApplicationListItemVO;
import interview.matching.model.vo.JobApplicationVO;
import interview.matching.model.vo.MyApplicationListItemVO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 简历投递与初筛记录表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */
public interface JobApplicationsMapper extends BaseMapper<JobApplications> {

    IPage<JobApplicationListBO> pageApplicationsWithJoin(IPage<?> page,
                                                         @Param("enterpriseId") Long enterpriseId,
                                                         @Param("jobId") Long jobId,
                                                         @Param("status") JobApplicationStatus status);

    JobApplicationBO getApplicationWithJoin(@Param("enterpriseId") Long enterpriseId,
                                            @Param("applicationId") Long applicationId);

    IPage<MyApplicationListBO> pageMyApplicationsWithJoin(IPage<?> page,
                                                          @Param("candidateId") Long candidateId,
                                                          @Param("status") JobApplicationStatus status);
}
