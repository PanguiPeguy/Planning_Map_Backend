package com.enspy.tripplanning.poi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Réponse paginée pour les POI")
public class PoiPageResponse {

    @Schema(description = "Liste des POI de la page courante")
    private List<PoiDTO> content;

    @Schema(description = "Numéro de la page actuelle (commence à 0)", example = "0")
    private Integer page;

    @Schema(description = "Taille de la page", example = "20")
    private Integer size;

    @Schema(description = "Nombre total d'éléments", example = "150")
    private Long totalElements;

    @Schema(description = "Nombre total de pages", example = "8")
    private Integer totalPages;
}