package com.weather.alarm.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.HandlerInterceptor
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // 정적 리소스 핸들러 추가
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(3600)

        // favicon 특별 처리
        registry.addResourceHandler("/favicon.ico", "/favicon.svg")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(86400)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(object : HandlerInterceptor {
            override fun preHandle(
                request: HttpServletRequest,
                response: HttpServletResponse,
                handler: Any
            ): Boolean {
                // CSP 헤더 추가 - Font Awesome CDN 허용
                response.setHeader(
                    "Content-Security-Policy",
                    "default-src 'self'; " +
                    "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://use.fontawesome.com; " +
                    "font-src 'self' https://cdnjs.cloudflare.com https://use.fontawesome.com; " +
                    "img-src 'self' data:; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "connect-src 'self'"
                )
                return true
            }
        })
    }
}
