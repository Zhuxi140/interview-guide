package interview.api.system.dto;

/**
 * 企业公开展示信息。
 *
 * @param id        企业 ID
 * @param name      企业全称
 * @param shortName 企业简称
 * @param industry  所属行业
 * @param scale     企业规模
 * @param logoUrl   企业 Logo
 */
public record EnterprisePublicProfileDTO(
        Long id,
        String name,
        String shortName,
        String industry,
        String scale,
        String logoUrl
) {
}
