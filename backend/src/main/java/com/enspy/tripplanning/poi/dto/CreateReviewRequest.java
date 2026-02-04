package com.enspy.tripplanning.poi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête création avis")
public class CreateReviewRequest {

    @Schema(description = "Note (0.0 à 5.0)", example = "4.5", required = true)
    @NotNull(message = "Note requise")
    @DecimalMin(value = "0.0", message = "Note >= 0")
    @DecimalMax(value = "5.0", message = "Note <= 5")
    private Double rating;

    @Schema(description = "Commentaire", example = "Excellent séjour !")
    @Size(max = 1000, message = "Commentaire max 1000 caractères")
    private String comment;

    @Schema(description = "URLs photos")
    private List<String> images;
}