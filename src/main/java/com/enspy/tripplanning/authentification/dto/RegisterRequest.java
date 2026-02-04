// src/main/java/com/enspy/tripplanning/Authentification/dto/RegisterRequest.java

package com.enspy.tripplanning.authentification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Données pour l'inscription d'un nouvel utilisateur")
public class RegisterRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 20)
    @Schema(description = "Nom d'utilisateur unique", example = "negou")
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    @Schema(description = "Adresse email valide", example = "negou@gmail.com", format = "email")
    private String email;

    @Schema(description = "Nom de l'entreprise (optionnel)", example = "Ma Société")
    private String companyName;

    @Schema(description = "Numéro de téléphone", example = "+237699999999")
    private String phone;

    @Schema(description = "Ville de résidence", example = "Yaoundé")
    private String city;

    @Schema(description = "Mode de transport préféré", example = "Voiture")
    private String transportmode;

    @Schema(description = "URL de la photo de profil (optionnel)", example = "https://example.com/photo.jpg")
    private String profilePhotoUrl;


    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit faire au moins 6 caractères")

    private String password;
}