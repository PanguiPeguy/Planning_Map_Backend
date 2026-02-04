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
 * DTO - TripWaypointDTO
 * ================================================================
 * Information d'un waypoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Étape du voyage")
public class TripWaypointDTO {

    @Schema(description = "ID du waypoint")
    private Long waypointId;

    @Schema(description = "Ordre dans le voyage", example = "1")
    private Integer orderIndex;

    @Schema(description = "Type", example = "WAYPOINT")
    private String waypointType;

    @Schema(description = "ID du POI (si applicable)")
    private Long poiId;

    @Schema(description = "Nom du POI ou custom", example = "Hotel Hilton")
    private String name;

    @Schema(description = "Latitude")
    private BigDecimal latitude;

    @Schema(description = "Longitude")
    private BigDecimal longitude;

    @Schema(description = "Heure d'arrivée prévue")
    private LocalDateTime plannedArrivalTime;

    @Schema(description = "Heure de départ prévue")
    private LocalDateTime plannedDepartureTime;

    @Schema(description = "Durée d'arrêt (minutes)", example = "60")
    private Integer plannedDurationMinutes;

    @Schema(description = "Notes")
    private String notes;
}