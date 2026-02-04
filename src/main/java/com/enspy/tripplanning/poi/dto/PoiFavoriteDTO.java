package com.enspy.tripplanning.poi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informations sur un Favori")
public class PoiFavoriteDTO {
    private Long favoriteId;
    private Long poiId;
    private UUID userId;
    private String notes;
    private LocalDateTime createdAt;
}
