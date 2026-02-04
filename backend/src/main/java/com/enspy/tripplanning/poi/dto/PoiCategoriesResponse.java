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
@Schema(description = "Réponse contenant la liste des catégories de POI")
public class PoiCategoriesResponse {

    @Schema(description = "Liste des catégories de POI")
    private List<PoiCategoryDTO> categories;
}