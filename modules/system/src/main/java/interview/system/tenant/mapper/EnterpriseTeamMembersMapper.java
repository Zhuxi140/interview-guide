package interview.system.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.system.tenant.model.bo.TeamMemberItemBO;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.vo.TeamMemberItemVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 企业内部团队成员表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 */

@Mapper
public interface EnterpriseTeamMembersMapper extends BaseMapper<EnterpriseTeamMember> {


    Page<TeamMemberItemBO> getTeamMembersPage(Page<TeamMemberItemBO> page, Long enterpriseId);
}
