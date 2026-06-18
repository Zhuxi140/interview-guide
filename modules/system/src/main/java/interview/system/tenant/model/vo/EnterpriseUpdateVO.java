package interview.system.tenant.model.vo;

/**
 * @author zhuxi
 * @apiNote 更新企业信息响应
 */
public record EnterpriseUpdateVO(
    Long id,
    String name,
    String shortName,
    String industry
) {}
