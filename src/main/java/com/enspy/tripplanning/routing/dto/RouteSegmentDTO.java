package com.enspy.tripplanning.routing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Représente un segment de route dans l'itinéraire
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Segment de route de l'itinéraire")
public class RouteSegmentDTO {

    @Schema(description = "Numéro du segment (ordre)")
    private Integer segmentNumber;

    @Schema(description = "ID de l'arête")
    private Long edgeId;

    @Schema(description = "Nom de la rue")
    private String streetName;

    @Schema(description = "Type de route", example = "primary")
    private String roadType;

    @Schema(description = "Distance du segment en kilomètres")
    private Double distanceKm;

    @Schema(description = "Temps de parcours du segment en secondes")
    private Integer timeSeconds;

    @Schema(description = "Vitesse maximale autorisée (km/h)")
    private Integer maxSpeedKmh;

    @Schema(description = "Point de départ du segment")
    private RoutePointDTO startPoint;

    @Schema(description = "Point d'arrivée du segment")
    private RoutePointDTO endPoint;

    @Schema(description = "Instruction de navigation", example = "Suivez Avenue Kennedy pendant 5.2 km")
    private String instruction;
}
