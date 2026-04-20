package com.kidslearn.api.config;

import com.kidslearn.api.interceptor.AdminAuthInterceptor;
import com.kidslearn.api.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 - 统一注册业务API和管理后台API的拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 业务API拦截器：/api/v1/**
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/api/v1/public/**"
                );

        // 管理后台API拦截器：/admin/api/v1/**
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/api/v1/**")
                .excludePathPatterns(
                        "/admin/api/v1/auth/**"
                );
    }
}
