package com.ibizabroker.bibliotheque.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfiguration {

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    /**
     * Ajoute pour PATCH /api/reservations/{id}/annuler. La liste des methodes
     * etant explicite, PATCH etait rejete au preflight : le navigateur aurait
     * bloque l'annulation depuis l'interface Angular, alors que le meme appel
     * fonctionne depuis curl ou Swagger UI, qui ne declenchent pas de preflight.
     */
    private static final String PATCH = "PATCH";

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods(GET, POST, PUT, PATCH, DELETE)
                        .allowedHeaders("*")
                        .allowedOriginPatterns("*")
                        .allowCredentials(true);
            }
        };
    }
}