package interview.system.tenant.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @apiNote 企业详情响应
 */
@Builder
public record EnterpriseDetailVO(
    Long id,
    String name,
    String shortName,
    String industry,
    String scale,
    String contactEmail,
    String contactPhone,
    Integer status,
    String logoUrl,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt
) {}
