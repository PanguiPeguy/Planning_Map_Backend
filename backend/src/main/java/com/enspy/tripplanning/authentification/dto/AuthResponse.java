package com.enspy.tripplanning.authentification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Réponse après connexion ou inscription réussie")
public record AuthResponse(
                @Schema(description = "ID unique de l'utilisateur", example = "a1b2c3d4-...") UUID id,
                @Schema(description = "Nom d'utilisateur", example = "negou") String username,
                @Schema(description = "Email de l'utilisateur", example = "negou@gmail.com") String email,
                @Schema(description = "Rôle de l'utilisateur", example = "ADMIN") String role,
                @Schema(description = "Token JWT à utiliser dans le header Authorization: Bearer <token>") String token) {
}