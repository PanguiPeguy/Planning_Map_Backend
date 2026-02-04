package com.enspy.tripplanning.poi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Avis POI")
public class ReviewDTO {

    @Schema(description = "ID avis")
    private Long reviewId;

    @Schema(description = "ID POI")
    private Long poiId;

    @Schema(description = "ID auteur")
    private UUID userId;

    @Schema(description = "Nom auteur")
    private String username;

    @Schema(description = "Note")
    private BigDecimal rating;

    @Schema(description = "Commentaire")
    private String comment;

    @Schema(description = "Photos")
    private List<String> images;

    @Schema(description = "Visite vérifiée GPS")
    private Boolean isVerifiedVisit;

    @Schema(description = "Nombre de votes utiles")
    private Integer helpfulCount;

    @Schema(description = "Date création")
    private LocalDateTime createdAt;

    @Schema(description = "Temps écoulé", example = "il y a 2 heures")
    private String timeAgo;
}