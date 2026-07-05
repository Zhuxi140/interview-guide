package interview.system.tenant;

import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.vo.EnterpriseCreateVO;
import interview.system.tenant.model.vo.EnterpriseListItemVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 企业转换器
 */

@Mapper(componentModel = "spring")
public interface EnterpriseCovert {

    EnterpriseCreateVO convertToEnterpriseCreateVO(EnterpriseCreateBO bo);

    List<EnterpriseListItemVO> BOCovertToEnterpriseListItemVO(List<ListUserEnterprisesBO> voList);
}
