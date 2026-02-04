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
 * DTO - UpdateMemberRoleRequest
 * ================================================================
 * Requête de modification du rôle d'un membre
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de modification du rôle")
public class UpdateMemberRoleRequest {

    @Schema(description = "Nouveau rôle", example = "VIEWER", required = true)
    @NotBlank(message = "Le rôle est requis")
    @Pattern(regexp = "EDITOR|VIEWER", message = "Rôle invalide")
    private String newRole;
}