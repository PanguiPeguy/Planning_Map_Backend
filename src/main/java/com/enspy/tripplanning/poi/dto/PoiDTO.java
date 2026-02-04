package com.enspy.tripplanning.poi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informations complètes d'un Point d'Intérêt")
public class PoiDTO {

    @Schema(description = "ID unique du POI", example = "1")
    private Long poiId;

    @Schema(description = "Nom du POI", example = "Hotel Central", required = true)
    @NotBlank(message = "Le nom du POI est requis")
    private String name;

    @Schema(description = "Description du POI", example = "Hôtel 3 étoiles au centre-ville")
    private String description;

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

    @Schema(description = "Catégorie du POI")
    private PoiCategoryDTO category;

    @Schema(description = "Adresse complète", example = "Avenue de la République")
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

    @Schema(description = "Pays", example = "Cameroun")
    private String addressCountry;

    @Schema(description = "Numéro de téléphone", example = "+237123456789")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Format de téléphone invalide")
    private String phone;

    @Schema(description = "Note moyenne", example = "4.2")
    @DecimalMin(value = "0.0", message = "La note doit être >= 0")
    @DecimalMax(value = "5.0", message = "La note doit être <= 5")
    private Double rating;

    @Schema(description = "Horaires d'ouverture", example = "{\"monday\": \"06:00-22:00\", \"tuesday\": \"06:00-22:00\"}")
    private Map<String, String> openingHours;

    @Schema(description = "Services disponibles", example = "[\"wifi\", \"parking\", \"restaurant\"]")
    private List<String> services;

    @Schema(description = "Fourchette de prix", example = "€€")
    private String priceRange;

    @Schema(description = "Nombre de commentaires", example = "10")
    private Integer reviewCount;

    @Schema(description = "Nombre de likes", example = "25")
    private Integer likeCount;

    @Schema(description = "Nombre d'ajouts en favoris", example = "15")
    private Integer favoriteCount;

    @Schema(description = "Si l'utilisateur connecté a aimé ce POI", example = "true")
    @JsonProperty("isLiked")
    private boolean isLiked;

    @Schema(description = "Si l'utilisateur connecté a mis ce POI en favoris", example = "false")
    @JsonProperty("isFavorite")
    private boolean isFavorite;

    @Schema(description = "URL de l'image principale", example = "https://res.cloudinary.com/...")
    private String imageUrl;

    @Schema(description = "Métadonnées supplémentaires")
    private Map<String, Object> metadata;
}
