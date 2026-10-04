package it.polito.wa2.fileservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private final Logger logger = LoggerFactory.getLogger(WebCorsConfig.class);

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        logger.info("addCorsMappings");
        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedOrigins("*")
                .allowedHeaders("Origin", "X-Requested-With", "Content-Type", "Accept", "Final-Length", "Upload-Metadata", "Upload-Length", "Tus-Resumable", "Upload-Offset", "Upload-Expires", "Upload-Checksum")
                .exposedHeaders("tus-version", "Tus-Resumable", "Tus-Extension", "Tus-Max-Size", "Location", "Upload-Offset", "Upload-Length", "Upload-Metadata", "Upload-Expires", "Upload-Checksum")
                .maxAge(3600);
    }
}
