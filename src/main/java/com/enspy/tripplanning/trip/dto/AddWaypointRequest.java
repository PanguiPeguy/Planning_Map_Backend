package com.enspy.tripplanning.trip.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * DTO - AddWaypointRequest
 * ================================================================
 * Requête d'ajout d'un waypoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête d'ajout d'un waypoint")
public class AddWaypointRequest {

    @Schema(description = "ID du POI (si POI existant)")
    private Long poiId;

    @Schema(description = "Nom custom (si pas de POI)", example = "Chez Grand-mère")
    private String customName;

    @Schema(description = "Latitude custom (si pas de POI)", example = "3.8667")
    private BigDecimal customLatitude;

    @Schema(description = "Longitude custom (si pas de POI)", example = "11.5167")
    private BigDecimal customLongitude;

    @Schema(description = "Type de waypoint", example = "WAYPOINT")
    private String waypointType;

    @Schema(description = "Ordre dans le voyage", example = "1")
    private Integer orderIndex;

    @Schema(description = "Heure d'arrivée prévue")
    private LocalDateTime plannedArrivalTime;

    @Schema(description = "Durée d'arrêt (minutes)", example = "60")
    private Integer plannedDurationMinutes;

    @Schema(description = "Notes")
    private String notes;
}