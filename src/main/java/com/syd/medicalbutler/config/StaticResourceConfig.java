package com.syd.medicalbutler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {


    @Value("${avatar.baseDir:D:/MedicalButler/uploads/avatars}")
    private String avatarBaseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String resourceLocation = "file:" + avatarBaseDir.replace("\\", "/") + "/";
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600); // 可选：缓存时间（秒）
    }
}