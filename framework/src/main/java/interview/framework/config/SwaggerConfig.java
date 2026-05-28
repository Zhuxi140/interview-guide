package interview.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhuxi
 * @apiNote Swagger 配置
 * @since 2026-05-26
 */

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI智能面试系统")
                        .version("1.0.0")
                        .description("AI智能面试系统")
                        .contact(new Contact().name("zhuxi"))
                );
    }
}
