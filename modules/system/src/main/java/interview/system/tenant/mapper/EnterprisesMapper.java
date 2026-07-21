package interview.system.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.system.tenant.model.bo.UserEnterprisesBO;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 对企业联系方式变更获取 PostgreSQL 事务级咨询锁
     * @param lockKey 咨询锁键
     */
    @Select("SELECT pg_advisory_xact_lock(#{lockKey})")
    void lockEnterpriseContact(Long lockKey);
}
