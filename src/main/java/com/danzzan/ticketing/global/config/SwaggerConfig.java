package com.danzzan.ticketing.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DANSPOT Ticketing API")
                        .description("단국대학교 축제 티켓팅 서비스 API")
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",   //Swagger에 Bearer 인증 스키마 등록
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .description("관리자용 임시 Bearer 토큰 (opaque token)")));
    }
}
