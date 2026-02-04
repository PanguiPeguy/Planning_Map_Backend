package com.enspy.tripplanning.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "API Système de Planification d'Itinéraires", version = "1.0.0", description = """
                API REST pour le système de planification d'itinéraires avec Points d'Intérêt.

                Cette API permet de :
                - Gérer les Points d'Intérêt (POI)
                - Gérer les catégories de POI
                - Rechercher des POI par proximité, catégorie ou texte
                - Récupérer les POI dans une zone géographique

                **Technologies utilisées:**
                - Spring Boot WebFlux (Reactive)
                - PostgreSQL + PostGIS
                - R2DBC (Reactive Database Connectivity)
                - JWT pour l'authentification
                """, contact = @Contact(name = "TEFA NEO Team", email = "contact@tefa-neo.com"), license = @License(name = "MIT License", url = "https://opensource.org/licenses/MIT")), servers = {
                @Server(url = "http://localhost:8080", description = "Serveur de développement"),
                @Server(url = "https://api.itineraire.com", description = "Serveur de production")
})
@SecurityScheme(name = "bearer-jwt", description = "Authentification JWT (JSON Web Token). Format: 'Bearer {token}'", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {
}