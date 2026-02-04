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
 * DTO - InviteMemberRequest
 * ================================================================
 * Requête d'invitation d'un membre
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête d'invitation d'un membre")
public class InviteMemberRequest {

    @Schema(description = "Email de l'utilisateur à inviter", required = true)
    @NotBlank(message = "L'email est requis")
    @Email(message = "Email invalide")
    private String email;

    @Schema(description = "Rôle à attribuer", example = "EDITOR")
    @NotBlank(message = "Le rôle est requis")
    @Pattern(regexp = "EDITOR|VIEWER", message = "Rôle invalide (EDITOR ou VIEWER)")
    private String role;
}