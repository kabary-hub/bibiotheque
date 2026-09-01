package com.ibizabroker.bibliotheque.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadonnees affichees en tete de Swagger UI.
 *
 * springdoc-openapi decouvre les controleurs tout seul ; ce bean ne sert qu'a
 * remplacer le titre par defaut (« OpenAPI definition ») par quelque chose de
 * lisible.
 *
 * Swagger UI : http://localhost:8080/swagger-ui.html
 * Contrat brut : http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI bibliothequeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Gestion de Bibliotheque - API")
                .version("v1")
                .description("Livres, emprunts, adherents et reservations."));
    }
}
