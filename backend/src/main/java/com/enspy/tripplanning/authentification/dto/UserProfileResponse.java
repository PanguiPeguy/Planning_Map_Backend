package com.enspy.tripplanning.authentification.dto;

import com.enspy.tripplanning.authentification.entity.User.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Schema(description = "Détails complets du profil utilisateur")
public record UserProfileResponse(
    @Schema(description = "Identifiant unique")
    UUID id,
    
    @Schema(description = "Nom d'utilisateur")
    String username,
    
    @Schema(description = "Adresse email")
    String email,
    
    @Schema(description = "Nom de l'entreprise")
    String companyName,
    
    @Schema(description = "Numéro de téléphone")
    String phone,
    
    @Schema(description = "Ville")
    String city,
    
    @Schema(description = "Mode de transport")
    String transportmode,
    
    @Schema(description = "URL photo profil")
    String profilePhotoUrl,
    
    @Schema(description = "Rôle système")
    UserRole role,
    
    @Schema(description = "Date création compte")
    LocalDateTime createdAt
) {}
