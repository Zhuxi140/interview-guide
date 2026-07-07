package interview.system.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.tenant.model.bo.UserEnterprisesBO;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) Mapper 接口
 * </p>
 *
 * @author zhuxi
 */
@Mapper
public interface EnterprisesMapper extends BaseMapper<Enterprise> {

    List<UserEnterprisesBO> getListUserEnterprises(Long userId);

    Enterprise getEnterpriseDetail(Long enterpriseId);
}
