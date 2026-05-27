package org.example.skillsspringai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pptDir = System.getProperty("user.dir") + File.separator + "ppt" + File.separator;
        new File(pptDir).mkdirs();
        registry.addResourceHandler("/ppt/**")
                .addResourceLocations("file:" + pptDir);
    }
}
