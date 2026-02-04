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
 * DTO - TripMemberDTO
 * ================================================================
 * Information d'un membre
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Membre collaborateur")
public class TripMemberDTO {

    @Schema(description = "ID utilisateur")
    private UUID userId;

    @Schema(description = "Nom d'utilisateur", example = "alice")
    private String username;

    @Schema(description = "Email", example = "alice@example.com")
    private String email;

    @Schema(description = "Rôle", example = "EDITOR")
    private String role;

    @Schema(description = "Date d'ajout")
    private LocalDateTime joinedAt;

    @Schema(description = "Dernière activité")
    private LocalDateTime lastActivityAt;

    @Schema(description = "Notifications activées", example = "true")
    private Boolean notificationsEnabled;
}