package com.example.mail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Autowired
    private SessionAuthorizationInterceptor sessionAuthorizationInterceptor;

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH,HEAD}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Value("${upload.path:C:/Uploads/}")
    private String uploadPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsRegistration registration = registry.addMapping("/**")
                .allowedMethods(allowedMethods.split(","))
                .allowedHeaders(allowedHeaders.split(","))
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);

        if (allowedOrigins == null || allowedOrigins.trim().isEmpty() || "*".equals(allowedOrigins.trim()) || allowedOrigins.contains("*")) {
            registration.allowedOriginPatterns("*");
        } else {
            registration.allowedOrigins(allowedOrigins.split(","));
        }
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(allowCredentials);

        if (allowedOrigins == null || allowedOrigins.trim().isEmpty() || "*".equals(allowedOrigins.trim()) || allowedOrigins.contains("*")) {
            config.addAllowedOriginPattern("*");
        } else {
            for (String origin : allowedOrigins.split(",")) {
                if (!origin.trim().isEmpty()) {
                    config.addAllowedOrigin(origin.trim());
                }
            }
        }

        for (String method : allowedMethods.split(",")) {
            if (!method.trim().isEmpty()) {
                config.addAllowedMethod(method.trim());
            }
        }

        if (allowedHeaders == null || allowedHeaders.trim().isEmpty() || "*".equals(allowedHeaders.trim())) {
            config.addAllowedHeader("*");
        } else {
            for (String header : allowedHeaders.split(",")) {
                if (!header.trim().isEmpty()) {
                    config.addAllowedHeader(header.trim());
                }
            }
        }

        config.setMaxAge(maxAge);
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        String base = uploadPath != null ? uploadPath.trim() : "C:/Uploads/";
        if (!base.endsWith("/") && !base.endsWith("\\")) {
            base += "/";
        }
        String fileLocation = "file:" + base.replace("\\", "/");
        if (!fileLocation.endsWith("/")) {
            fileLocation += "/";
        }
        registry.addResourceHandler("/Inbound/**")
                .addResourceLocations(fileLocation + "Inbound/");
        registry.addResourceHandler("/inboundattachment/**")
                .addResourceLocations(fileLocation + "Inbound/");
        registry.addResourceHandler("/Outbound/**")
                .addResourceLocations(fileLocation + "Outbound/");

        // Ensure Swagger UI and webjars static resources are served
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionAuthorizationInterceptor)
                .addPathPatterns("/email-service/sbi/**")
                .excludePathPatterns(
                        "/email-service/sbi/session-auth/AgentDesktopLogin",
                        "/email-service/sbi/session-auth/login",
                        "/email-service/sbi/customerMail",
                        "/email-service/sbi/UpdateAuxDetails",
                        "/email-service/sbi/UpdateAgentContact",
                        "/email-service/sbi/UnableToReadContact",
                        "/email-service/sbi/session-auth/AgentDesktopLogoff",
                        "/email-service/sbi/agent-sessions/**"
                );
    }
}
