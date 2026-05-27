package interview.system.tenant.vo;

/**
 * @author zhuxi
 * @apiNote 创建企业响应
 * @since 2026/5/27 14:05
 */
public record EnterpriseCreateVO(
    Long id,
    String name,
    String shortName,
    Integer status
) {}
