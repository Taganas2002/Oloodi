package com.oloodi.livestaffing.dev;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Local/demo CORS so the Vite app (and direct EventSource against :8080) can call the API.
 *
 * <p>Uses a {@link CorsFilter} so preflight OPTIONS and rejected-origin handling stay consistent.
 * Spring returns HTTP 403 when the request Origin is not allowed — that surfaces in the Manager UI
 * as "Request failed with status 403".
 *
 * <p>Allowed local origins include {@code http://localhost:5173}, {@code http://127.0.0.1:5173},
 * {@code http://localhost:3000}, {@code http://127.0.0.1:3000} (any port via patterns).
 *
 * <p>Requires profile {@code local} or {@code demo}. Restart the backend after changing this.
 */
@Configuration
@Profile({"local", "demo"})
public class LocalCorsConfig {

    @Bean
    public CorsFilter localCorsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false);
        // Patterns cover Vite (:5173) / CRA (:3000) and any other local port.
        // (Spring does not allow mixing allowedOrigins + allowedOriginPatterns.)
        config.setAllowedOriginPatterns(
                List.of("http://localhost:*", "http://127.0.0.1:*", "http://[::1]:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
