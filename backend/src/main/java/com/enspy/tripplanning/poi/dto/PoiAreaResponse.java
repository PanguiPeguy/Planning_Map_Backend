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
@Schema(description = "Réponse contenant les POI dans une zone géographique")
public class PoiAreaResponse {

    @Schema(description = "Liste des POI trouvés dans la zone")
    private List<PoiDTO> pois;
}
