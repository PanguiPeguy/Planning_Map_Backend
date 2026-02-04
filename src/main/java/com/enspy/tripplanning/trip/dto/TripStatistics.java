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
 * DTO - TripStatistics
 * ================================================================
 * Statistiques calculées d'un voyage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Statistiques du voyage")
public class TripStatistics {

    @Schema(description = "Distance totale (km)", example = "245.5")
    private BigDecimal totalDistanceKm;

    @Schema(description = "Durée totale (minutes)", example = "420")
    private Integer totalDurationMinutes;

    @Schema(description = "Durée formatée", example = "7h 00min")
    private String formattedDuration;

    @Schema(description = "Coût estimé (FCFA)", example = "150000")
    private BigDecimal estimatedCost;

    @Schema(description = "Nombre de waypoints", example = "5")
    private Integer waypointCount;

    @Schema(description = "Nombre de membres", example = "3")
    private Integer memberCount;

    @Schema(description = "Nombre de jours", example = "3")
    private Long durationDays;
}
