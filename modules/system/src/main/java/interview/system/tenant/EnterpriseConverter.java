package interview.system.tenant;

import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.vo.EnterpriseCreateVO;
import interview.system.tenant.model.vo.EnterpriseListItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 企业转换器
 */

@Mapper(componentModel = "spring")
public interface EnterpriseConverter {

    EnterpriseCreateVO convertToEnterpriseCreateVO(EnterpriseCreateBO bo);

    @Mapping(source = "enterpriseId",target = "id")
    List<EnterpriseListItemVO> BOCovertToEnterpriseListItemVO(List<ListUserEnterprisesBO> boList);
}
