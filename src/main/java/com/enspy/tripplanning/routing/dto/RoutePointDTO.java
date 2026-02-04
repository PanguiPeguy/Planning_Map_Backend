package com.enspy.tripplanning.routing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ============================================
// NESTED DTOs
// ============================================

/**
 * Représente un point de l'itinéraire (départ/arrivée/waypoint)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Point géographique de l'itinéraire")
public class RoutePointDTO {

    @Schema(description = "ID du nœud correspondant")
    private Long nodeId;

    @Schema(description = "Latitude")
    private Double latitude;

    @Schema(description = "Longitude")
    private Double longitude;

    @Schema(description = "Nom du lieu")
    private String name;

    @Schema(description = "Type de point", example = "start")
    private String type; // start, end, waypoint
}