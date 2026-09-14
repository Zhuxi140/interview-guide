package interview.framework.security.Desensitize;

import cn.hutool.core.util.DesensitizedUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

/**
 * @author zhuxi
 * @apiNote 数据脱敏类型枚举
 */

@AllArgsConstructor
@Getter
public enum DesensitizeType {
    PHONE(DesensitizedUtil::mobilePhone),
    ID_CARD(s -> DesensitizedUtil.idCardNum(s, 1, 2)),
    BANK_CARD(DesensitizedUtil::bankCard),
    CUSTOM_RULE(s -> s.replaceAll("(?<=\\w{3})\\w(?=\\w{4})", "*")),
    /**
     * 凭证类值固定掩码：不保留原文任何片段（密钥/密码即使部分泄露也有风险）。
     * 适用于动态键 Map 中的凭证字段掩码（如渠道配置 config_json）。
     */
    SECRET(s -> "******");

    private final Function<String,String> desensitizeFunction;
}
