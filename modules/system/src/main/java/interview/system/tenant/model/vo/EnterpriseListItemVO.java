package interview.system.tenant.model.vo;

import lombok.Builder;

/**
 * @author zhuxi
 * @apiNote 企业列表项响应
 */
@Builder
public record EnterpriseListItemVO(
    Long id,
    String name,
    String shortName,
    String industry,
    Integer status,
    String roleCode,
    Integer memberCount
) {}
