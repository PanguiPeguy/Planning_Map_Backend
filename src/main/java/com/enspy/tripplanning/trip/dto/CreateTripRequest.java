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
 * DTO - CreateTripRequest
 * ================================================================
 * Requête de création d'un nouveau voyage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de création d'un voyage")
public class CreateTripRequest {

    @Schema(description = "Titre du voyage", example = "Road Trip Cameroun 2025", required = true)
    @NotBlank(message = "Le titre est requis")
    @Size(min = 3, max = 200, message = "Le titre doit faire entre 3 et 200 caractères")
    private String title;

    @Schema(description = "Description du voyage", example = "Découverte des routes nationales")
    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;

    @Schema(description = "Date de début", example = "2025-01-15")
    private LocalDate startDate;

    @Schema(description = "Date de fin", example = "2025-01-20")
    private LocalDate endDate;

    @Schema(description = "Voyage public ?", example = "false")
    @Builder.Default
    private Boolean isPublic = false;

    @Schema(description = "Mode collaboratif ?", example = "true")
    @Builder.Default
    private Boolean isCollaborative = false;

    @Schema(description = "Métadonnées personnalisées")
    private Map<String, Object> metadata;
}
