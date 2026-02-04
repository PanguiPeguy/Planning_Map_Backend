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
 * DTO - TripResponse
 * ================================================================
 * Réponse contenant les informations d'un voyage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informations complètes d'un voyage")
public class TripResponse {

    @Schema(description = "ID du voyage")
    private UUID tripId;

    @Schema(description = "Titre")
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Date de début")
    private LocalDate startDate;

    @Schema(description = "Date de fin")
    private LocalDate endDate;

    @Schema(description = "Statut", example = "DRAFT")
    private String status;

    @Schema(description = "Voyage public ?")
    private Boolean isPublic;

    @Schema(description = "Mode collaboratif ?")
    private Boolean isCollaborative;

    @Schema(description = "Token de partage")
    private String shareToken;

    @Schema(description = "Statistiques du voyage")
    private TripStatistics statistics;

    @Schema(description = "Propriétaire")
    private TripOwnerInfo owner;

    @Schema(description = "Waypoints du voyage")
    private List<TripWaypointDTO> waypoints;

    @Schema(description = "Membres collaborateurs")
    private List<TripMemberDTO> members;

    @Schema(description = "Métadonnées")
    private Map<String, Object> metadata;

    @Schema(description = "Date de création")
    private LocalDateTime createdAt;

    @Schema(description = "Date de mise à jour")
    private LocalDateTime updatedAt;
}