package com.enspy.tripplanning.poi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Données pour créer un nouveau POI")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePoiRequest {

    @Schema(description = "Nom du POI", example = "Station Total", required = true)
    @NotBlank(message = "Le nom du POI est requis")
    private String name;

    @Schema(description = "Description du POI", example = "Station-service avec boutique")
    private String description;

    @Schema(description = "ID de la catégorie", example = "4", required = true)
    @NotNull(message = "L'ID de la catégorie est requis")
    private Long categoryId;

    @Schema(description = "Latitude (coordonnée GPS)", example = "3.8667", required = true)
    @NotNull(message = "La latitude est requise")
    @DecimalMin(value = "-90.0", message = "La latitude doit être >= -90")
    @DecimalMax(value = "90.0", message = "La latitude doit être <= 90")
    private Double latitude;

    @Schema(description = "Longitude (coordonnée GPS)", example = "11.5167", required = true)
    @NotNull(message = "La longitude est requise")
    @DecimalMin(value = "-180.0", message = "La longitude doit être >= -180")
    @DecimalMax(value = "180.0", message = "La longitude doit être <= 180")
    private Double longitude;

    @Schema(description = "Adresse complète", example = "Route Nationale 1")
    private String address;

    @Schema(description = "Rue", example = "Boulevard du 20 Mai")
    private String addressStreet;

    @Schema(description = "Ville", example = "Yaoundé")
    private String addressCity;

    @Schema(description = "Code postal", example = "BP 123")
    private String addressPostalCode;

    @Schema(description = "Région", example = "Centre")
    private String addressRegion;

    @Schema(description = "Quartier", example = "Bastos")
    private String addressNeighborhood;

    @Schema(description = "Numéro de téléphone", example = "+237123456789")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Format de téléphone invalide")
    private String phone;

    @Schema(description = "Horaires d'ouverture", example = "{\"monday\": \"06:00-22:00\", \"tuesday\": \"06:00-22:00\"}")
    private Map<String, String> openingHours;

    @Schema(description = "Services disponibles", example = "[\"fuel\", \"shop\", \"restroom\"]")
    private List<String> services;

    @Schema(description = "Fourchette de prix", example = "€")
    private String priceRange;
}