package com.enspy.tripplanning.poi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informations d'une catégorie de POI")
public class PoiCategoryDTO {

    @Schema(description = "ID unique de la catégorie", example = "1")
    private Long categoryId;

    @Schema(description = "Nom de la catégorie", example = "Péage", required = true)
    @NotBlank(message = "Le nom de la catégorie est requis")
    private String name;

    @Schema(description = "Description de la catégorie", example = "Barrages et points de péage")
    private String description;

    @Schema(description = "Icône de la catégorie", example = "toll")
    private String icon;

    @Schema(description = "Couleur hexadécimale de la catégorie", example = "#FF5733")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Format de couleur invalide")
    private String color;
}