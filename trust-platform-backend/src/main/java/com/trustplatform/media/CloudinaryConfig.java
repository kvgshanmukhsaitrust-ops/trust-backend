package com.trustplatform.media;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        String url = (cloudinaryUrl == null || cloudinaryUrl.trim().isEmpty()) 
                ? System.getenv("CLOUDINARY_URL") 
                : cloudinaryUrl;

        if (url == null || url.trim().isEmpty()) {
            return null; // Graceful fallback: service will detect null and use local storage / warn
        }
        return new Cloudinary(url);
    }
}
