package com.testexecutionplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许所有源访问
        config.addAllowedOriginPattern("*");
        // 允许所有HTTP方法
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 移除允许携带凭证的设置，简化前端配置
        // config.setAllowCredentials(true);
        
        // 为所有API路径配置CORS
        source.registerCorsConfiguration("/api/**", config);
        
        return new CorsFilter(source);
    }
}
