package com.enspy.tripplanning.routing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Statistiques de performance de l'algorithme A*
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Statistiques de calcul de l'itinéraire")
public class RouteStatisticsDTO {

    @Schema(description = "Nombre de nœuds explorés par l'algorithme")
    private Integer nodesExplored;

    @Schema(description = "Nombre de nœuds dans le graphe")
    private Integer totalNodesInGraph;

    @Schema(description = "Temps d'exécution de l'algorithme (ms)")
    private Long computationTimeMs;

    @Schema(description = "Algorithme utilisé", example = "A* avec heuristique euclidienne")
    private String algorithm;

    @Schema(description = "Taux d'exploration", example = "0.05")
    private Double explorationRate; // nodesExplored / totalNodes
}