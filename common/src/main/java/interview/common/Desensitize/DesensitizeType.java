package interview.common.Desensitize;

import cn.hutool.core.util.DesensitizedUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

/**
 * @author zhuxi
 * @apiNote 数据脱敏类型枚举
 * @since 2026/5/27 14:05
 */

@AllArgsConstructor
@Getter
public enum DesensitizeType {
    PHONE(DesensitizedUtil::mobilePhone),
    ID_CARD(s -> DesensitizedUtil.idCardNum(s, 1, 2)),
    BANK_CARD(DesensitizedUtil::bankCard),
    CUSTOM_RULE(s -> s.replaceAll("(?<=\\w{3})\\w(?=\\w{4})", "*"));

    private final Function<String,String> desensitizeFunction;
}
