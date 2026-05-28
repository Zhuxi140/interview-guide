package interview.system.tenant.model.vo;

/**
 * @author zhuxi
 * @apiNote 更新企业信息响应
 * @since 2026/5/27 14:05
 */
public record EnterpriseUpdateVO(
    Long id,
    String name,
    String shortName,
    String industry
) {}
