package com.enspy.tripplanning.routing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse contenant plusieurs options d'itinéraire.
 * Permet à l'utilisateur de comparer sa route personnalisée
 * avec la route directe optimale calculée par le système.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Réponse contenant plusieurs options d'itinéraire")
public class MultiRouteResponse {

    @Schema(description = "Indique si des chemins ont été trouvés")
    private Boolean found;

    @Schema(description = "Message d'erreur si aucun chemin trouvé")
    private String errorMessage;

    @Schema(description = "L'itinéraire personnalisé passant par les POIs choisis")
    private RouteResponse customRoute;

    @Schema(description = "L'itinéraire direct optimal entre départ et arrivée")
    private RouteResponse optimalRoute;
}
