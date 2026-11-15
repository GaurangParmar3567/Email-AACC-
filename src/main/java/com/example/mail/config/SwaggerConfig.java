package com.example.mail.config;

import com.example.mail.controller.AgentSessionController;
import com.example.mail.controller.ContactSoapController;
import com.example.mail.controller.SessionAuthController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
@EnableOpenApi
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30) // OpenAPI 3.0 specs
                .select()
                .apis(handler -> handler != null && (
                        ContactSoapController.class.equals(handler.declaringClass())
                        || SessionAuthController.class.equals(handler.declaringClass())
                ))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("AACC Contact & Authentication API")
                .description("API specification for Avaya AACC Contact services and Session Authentication.")
                .version("1.0.0")
                .build();
    }
}