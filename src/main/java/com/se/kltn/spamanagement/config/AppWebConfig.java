package com.se.kltn.spamanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class AppWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String reportPath = uploadPath("./generated-reports");
        registry.addResourceHandler("/generated-reports/**")
                .addResourceLocations("file:" + reportPath + "/");
    }

    private String uploadPath(String path) {
        Path uploadDir = Paths.get(path);
        return uploadDir.toFile().getAbsolutePath();
    }
}
