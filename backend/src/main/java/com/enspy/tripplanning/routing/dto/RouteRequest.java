package com.enspy.tripplanning.routing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ============================================
// REQUEST DTO
// ============================================

/**
 * Requête pour calculer un itinéraire entre deux points.
 * 
 * @author Pangui Peguy
 * @since 2024-12-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de calcul d'itinéraire")
public class RouteRequest {

    @Schema(description = "Latitude du point de départ", example = "3.8667", required = true)
    @NotNull(message = "La latitude de départ est requise")
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private Double startLatitude;

    @Schema(description = "Longitude du point de départ", example = "11.5167", required = true)
    @NotNull(message = "La longitude de départ est requise")
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private Double startLongitude;

    @Schema(description = "Latitude du point d'arrivée", example = "4.0511", required = true)
    @NotNull(message = "La latitude d'arrivée est requise")
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private Double endLatitude;

    @Schema(description = "Longitude du point d'arrivée", example = "9.7679", required = true)
    @NotNull(message = "La longitude d'arrivée est requise")
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private Double endLongitude;

    @Schema(description = "Liste optionnelle d'IDs de POI à inclure dans l'itinéraire")
    private List<Long> waypointPoiIds;

    @Schema(description = "Critère d'optimisation", example = "fastest", allowableValues = {"fastest", "shortest"})
    @Builder.Default
    private String optimizationCriteria = "fastest";

    @Schema(description = "Types de routes autorisés (motorway, primary, etc.)")
    private List<String> allowedRoadTypes;

    @Schema(description = "Vitesse minimale des routes (km/h)", example = "50")
    @Min(value = 0, message = "La vitesse doit être positive")
    private Integer minSpeedKmh;
}
