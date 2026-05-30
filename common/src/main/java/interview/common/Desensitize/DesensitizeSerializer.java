package interview.common.Desensitize;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.IOException;
import java.util.Optional;

/**
 * @author zhuxi
 * @apiNote 数据脱敏序列化
 * @since 2026/5/27 14:05
 */

@NoArgsConstructor
@AllArgsConstructor
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType type;


    /**
     *
     */
    @Override
    public void serialize(String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (!StrUtil.isNotBlank( value)){
            jsonGenerator.writeString( value);
            return;
        }
        String maskedValue = type.getDesensitizeFunction().apply( value);
        jsonGenerator.writeString( maskedValue);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }
        Desensitize annotation = property.getAnnotation(Desensitize.class);

        if (annotation == null) {
            annotation = property.getContextAnnotation(Desensitize.class);
        }

        if (annotation != null) {
            return new DesensitizeSerializer(annotation.type());
        }

        return prov.findValueSerializer(property.getType(), property);
    }
}
