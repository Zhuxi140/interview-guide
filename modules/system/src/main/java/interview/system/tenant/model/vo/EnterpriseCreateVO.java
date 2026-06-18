package interview.system.tenant.model.vo;

/**
 * @author zhuxi
 * @apiNote 创建企业响应
 */
public record EnterpriseCreateVO(
    Long id,
    String name,
    String shortName,
    Integer status
) {}
