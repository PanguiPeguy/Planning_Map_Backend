package com.enspy.tripplanning.routing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ============================================
// RESPONSE DTO
// ============================================

/**
 * Réponse contenant l'itinéraire calculé.
 * 
 * @author Thomas Djotio Ndié
 * @since 2024-12-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Résultat du calcul d'itinéraire")
public class RouteResponse {

    @Schema(description = "Indique si un chemin a été trouvé")
    private Boolean found;

    @Schema(description = "Message d'erreur si aucun chemin trouvé")
    private String errorMessage;

    @Schema(description = "Point de départ")
    private RoutePointDTO start;

    @Schema(description = "Point d'arrivée")
    private RoutePointDTO end;

    @Schema(description = "Liste ordonnée des segments de route")
    private List<RouteSegmentDTO> segments;

    @Schema(description = "Distance totale en kilomètres", example = "245.8")
    private Double totalDistanceKm;

    @Schema(description = "Temps de parcours total en secondes", example = "9000")
    private Integer totalTimeSeconds;

    @Schema(description = "Temps formaté (Xh Ymin)", example = "2h 30min")
    private String formattedTime;

    @Schema(description = "Nombre de segments de route", example = "12")
    private Integer segmentCount;

    @Schema(description = "Instructions de navigation étape par étape")
    private List<String> instructions;

    @Schema(description = "Statistiques de performance de l'algorithme")
    private RouteStatisticsDTO statistics;

    @Schema(description = "Géométrie du tracé (GeoJSON LineString)")
    private Object geometry;

    @Schema(description = "Géométrie encodée (Polyline)")
    private String geometryEncoded;
}