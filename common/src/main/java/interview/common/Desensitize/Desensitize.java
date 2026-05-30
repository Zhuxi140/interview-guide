package interview.common.Desensitize;

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * @author zhuxi
 * @apiNote 数据脱敏注解
 * @since 2026/5/27 14:05
 */


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {
    DesensitizeType type() default DesensitizeType.CUSTOM_RULE;
}
