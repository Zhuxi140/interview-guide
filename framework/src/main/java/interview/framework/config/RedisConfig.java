package interview.framework.config;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * @author zhuxi
 * @apiNote Redis配置
 * @since 2026/5/27 14:05
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);



        //类型验证器（放RCE）
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                // 允许Java对象
                .allowIfBaseType(Object.class)
                // 允许 interview.modules 包下的对象
                .allowIfSubType("interview.modules")
                .build();

        ObjectMapper objectMapper = new ObjectMapper().rebuild()
                .activateDefaultTyping(ptv)
                .build();

        GenericJacksonJsonRedisSerializer genericJacksonJsonRedisSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(genericJacksonJsonRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(genericJacksonJsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

}
