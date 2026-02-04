package com.enspy.tripplanning.routing.controller;

import com.enspy.tripplanning.routing.dto.MultiRouteResponse;
import com.enspy.tripplanning.routing.dto.RouteRequest;
import com.enspy.tripplanning.routing.service.RouteCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Contrôleur REST pour le calcul d'itinéraires.
 * 
 * Expose les endpoints pour:
 * - Calculer un itinéraire optimal entre deux points
 * - Calculer des itinéraires avec waypoints (POI)
 * 
 * @author Pangui Peguy
 * @version 1.0
 * @since 2024-12-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/routing")
@RequiredArgsConstructor
@Tag(name = "Routage", description = "API de calcul d'itinéraires optimaux")
public class RoutingController {

    private final RouteCalculatorService routeCalculatorService;

    /**
     * Calcule l'itinéraire optimal entre deux points.
     * 
     * ALGORITHME UTILISÉ: A* avec heuristique euclidienne
     * 
     * PERFORMANCE ATTENDUE:
     * - < 100 km : < 500 ms
     * - 100-500 km : < 2 secondes
     * - > 500 km : < 5 secondes
     * 
     * @param request Coordonnées de départ et d'arrivée
     * @return Itinéraire détaillé avec instructions
     */
    @Operation(summary = "Calculer un itinéraire", description = """
            Calcule le chemin optimal entre deux points géographiques en utilisant
            l'algorithme A* sur le réseau routier.

            L'API effectue automatiquement le "snapping" des coordonnées GPS
            sur le nœud le plus proche du graphe routier.

            **Optimisations:**
            - Heuristique euclidienne pour guider la recherche
            - Index spatiaux PostGIS pour performances
            - Complexité O(E log V) garantie

            **Exemple de requête Yaoundé → Douala:**
            ```json
            {
              "startLatitude": 3.8667,
              "startLongitude": 11.5167,
              "endLatitude": 4.0511,
              "endLongitude": 9.7679
            }
            ```
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plusieurs options d'itinéraire calculées", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MultiRouteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides"),
            @ApiResponse(responseCode = "404", description = "Aucun chemin trouvé"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
    @PostMapping(value = "/calculate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<MultiRouteResponse> calculateRoute(
            @Parameter(description = "Requête contenant les coordonnées de départ et d'arrivée", required = true) @Valid @RequestBody RouteRequest request) {
        log.info("POST /api/v1/routing/calculate - Calcul d'itinéraire multi-route demandé");

        return routeCalculatorService.calculateRoute(request)
                .doOnSuccess(response -> {
                    if (response.getFound()) {
                        log.info("Calcul multi-route réussi.");
                    } else {
                        log.warn("Aucun chemin trouvé: {}", response.getErrorMessage());
                    }
                })
                .doOnError(error -> {
                    log.error("Erreur critique lors du calcul de l'itinéraire", error);
                    // Log stack trace explicitly
                    error.printStackTrace();
                });
    }

    /**
     * Calcule un itinéraire simplifié à partir de paramètres GET.
     */
    @Operation(summary = "Calculer un itinéraire (méthode GET simplifiée)", description = "Version simplifiée pour tests rapides.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Itinéraire calculé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MultiRouteResponse.class)))
    })
    @GetMapping(value = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MultiRouteResponse> calculateRouteSimple(
            @Parameter(description = "Latitude de départ", example = "3.8667", required = true) @RequestParam Double startLat,

            @Parameter(description = "Longitude de départ", example = "11.5167", required = true) @RequestParam Double startLon,

            @Parameter(description = "Latitude d'arrivée", example = "4.0511", required = true) @RequestParam Double endLat,

            @Parameter(description = "Longitude d'arrivée", example = "9.7679", required = true) @RequestParam Double endLon) {
        log.info("GET /api/v1/routing/calculate - Calcul simplifié multi-route");

        RouteRequest request = RouteRequest.builder()
                .startLatitude(startLat)
                .startLongitude(startLon)
                .endLatitude(endLat)
                .endLongitude(endLon)
                .build();

        return routeCalculatorService.calculateRoute(request);
    }

    /**
     * Endpoint de test pour vérifier que le service est opérationnel.
     * 
     * @return Message de statut
     */
    @Operation(summary = "Test de santé du service de routage", description = "Vérifie que le service est opérationnel")
    @GetMapping("/health")
    public Mono<String> healthCheck() {
        return Mono.just("Service de routage opérationnel ✓");
    }
}