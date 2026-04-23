package com.zokomart.backend.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import com.zokomart.backend.admin.common.AdminAccessPolicy;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfiguration implements WebMvcConfigurer {

    private final AdminAccessPolicy adminAccessPolicy;

    public SaTokenConfiguration(AdminAccessPolicy adminAccessPolicy) {
        this.adminAccessPolicy = adminAccessPolicy;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    String requestMethod = SaHolder.getRequest().getMethod();
                    if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
                        return;
                    }
                    String requestPath = SaHolder.getRequest().getRequestPath();
                    if (requestPath == null || requestPath.isBlank()) {
                        return;
                    }
                    if (requestPath.startsWith("/admin/") || requestPath.startsWith("/merchant-admin/")) {
                        adminAccessPolicy.checkRouteAccess(requestPath);
                    }
                }))
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
