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
 * DTO - UpdateTripRequest
 * ================================================================
 * Requête de mise à jour d'un voyage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de mise à jour d'un voyage")
public class UpdateTripRequest {

    @Schema(description = "Nouveau titre")
    @Size(min = 3, max = 200)
    private String title;

    @Schema(description = "Nouvelle description")
    @Size(max = 2000)
    private String description;

    @Schema(description = "Nouvelle date de début")
    private LocalDate startDate;

    @Schema(description = "Nouvelle date de fin")
    private LocalDate endDate;

    @Schema(description = "Statut", example = "PLANNED")
    private String status;

    @Schema(description = "Visibilité publique")
    private Boolean isPublic;

    @Schema(description = "Mode collaboratif")
    private Boolean isCollaborative;
}
