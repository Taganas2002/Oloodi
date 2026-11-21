package com.oloodi.livestaffing.dev;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Local/demo CORS so the Vite app can open EventSource directly against :8080 in E2E. */
@Configuration
@Profile({"local", "demo"})
public class LocalCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://127.0.0.1:5173",
                        "http://localhost:5173",
                        "http://127.0.0.1:3000",
                        "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}
