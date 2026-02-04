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
 * DTO - TripOwnerInfo
 * ================================================================
 * Informations du propriétaire
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informations du propriétaire")
public class TripOwnerInfo {

    @Schema(description = "ID utilisateur")
    private UUID userId;

    @Schema(description = "Nom d'utilisateur", example = "johndoe")
    private String username;

    @Schema(description = "Email", example = "john@example.com")
    private String email;
}