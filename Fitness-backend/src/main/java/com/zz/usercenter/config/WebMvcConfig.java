package com.zz.usercenter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /api/files/** -> 服务器本地目录
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadPath);
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 聊天 SSE / StreamingResponseBody 可能持续数分钟，避免被 Spring MVC 默认异步超时提前中断
        configurer.setDefaultTimeout(300_000L);
    }
}
