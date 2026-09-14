package interview.framework.security.Desensitize;

import cn.hutool.core.util.StrUtil;

/**
 * 脱敏静态工具入口：服务层手动脱敏场景统一走这里。
 *
 * <p>与 {@link Desensitize} 注解（Jackson 序列化场景）共享
 * {@link DesensitizeType} 作为掩码算法的单一事实来源：
 * VO 静态字段用注解，动态值/手动处理用本工具，禁止再手写
 * {@code StrUtil.hide} / 星号字面量等散落实现。</p>
 *
 * @author zhuxi
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    /**
     * 按脱敏类型掩码；空白输入原样返回，由调用方决定空值语义。
     *
     * @param type  脱敏类型
     * @param value 原文（允许 null/空白）
     * @return 掩码后的值；输入空白时原样返回
     */
    public static String mask(DesensitizeType type, String value) {
        if (type == null || StrUtil.isBlank(value)) {
            return value;
        }
        return type.getDesensitizeFunction().apply(value);
    }
}
