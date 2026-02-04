package com.enspy.tripplanning.routing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente le résultat d'un calcul d'itinéraire.
 * 
 * Contient le chemin optimal trouvé par l'algorithme A*
 * avec toutes les informations nécessaires pour le rendu.
 * 
 * Selon la modélisation mathématique:
 * Un chemin P = ⟨v₀, v₁, ..., vₖ⟩ où:
 * - v₀ = nœud source
 * - vₖ = nœud destination
 * - Coût total = Σ w(vᵢ, vᵢ₊₁)
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2024-12-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    /**
     * Nœud de départ
     */
    private RoadNode startNode;

    /**
     * Nœud d'arrivée
     */
    private RoadNode endNode;

    /**
     * Séquence ordonnée des nœuds formant le chemin
     * P = ⟨v₀, v₁, ..., vₖ⟩
     */
    @Builder.Default
    private List<RoadNode> nodes = new ArrayList<>();

    /**
     * Séquence ordonnée des arêtes formant le chemin
     * Arête i connecte nodes[i] → nodes[i+1]
     */
    @Builder.Default
    private List<RoadEdge> edges = new ArrayList<>();

    /**
     * Distance totale en kilomètres
     * Somme de toutes les distances des arêtes
     */
    private Double totalDistanceKm;

    /**
     * Temps de parcours total en secondes
     * C'est le coût total du chemin: Σ w(vᵢ, vᵢ₊₁)
     */
    private Integer totalTimeSeconds;

    /**
     * Nombre de nœuds explorés par l'algorithme
     * Métrique de performance de A*
     */
    private Integer nodesExplored;

    /**
     * Temps d'exécution de l'algorithme en millisecondes
     * Permet de mesurer la performance
     */
    private Long computationTimeMs;

    /**
     * Indique si un chemin a été trouvé
     */
    private Boolean found;

    /**
     * Message d'erreur si aucun chemin trouvé
     */
    private String errorMessage;

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    /**
     * Ajoute un nœud au chemin
     * 
     * @param node Nœud à ajouter
     */
    public void addNode(RoadNode node) {
        if (this.nodes == null) {
            this.nodes = new ArrayList<>();
        }
        this.nodes.add(node);
    }

    /**
     * Ajoute une arête au chemin
     * 
     * @param edge Arête à ajouter
     */
    public void addEdge(RoadEdge edge) {
        if (this.edges == null) {
            this.edges = new ArrayList<>();
        }
        this.edges.add(edge);
    }

    /**
     * Calcule la distance totale en parcourant toutes les arêtes
     * 
     * @return Distance totale en km
     */
    public double calculateTotalDistance() {
        if (edges == null || edges.isEmpty()) {
            return 0.0;
        }

        double total = edges.stream()
                .mapToDouble(RoadEdge::getDistanceMetersOrCalculate)
                .sum();

        return total / 1000.0; // Conversion mètres → kilomètres
    }

    /**
     * Calcule le temps total en parcourant toutes les arêtes
     * 
     * @return Temps total en secondes
     */
    public int calculateTotalTime() {
        if (edges == null || edges.isEmpty()) {
            return 0;
        }

        return edges.stream()
                .mapToInt(edge -> edge.getTravelTimeSeconds() != null ? edge.getTravelTimeSeconds() : 0)
                .sum();
    }

    /**
     * Retourne le nombre de segments de route
     * 
     * @return Nombre d'arêtes
     */
    public int getSegmentCount() {
        return edges != null ? edges.size() : 0;
    }

    /**
     * Retourne le nombre d'étapes (nœuds intermédiaires)
     * 
     * @return Nombre de nœuds - 1
     */
    public int getStepCount() {
        return nodes != null ? Math.max(0, nodes.size() - 1) : 0;
    }

    /**
     * Formate le temps total en format lisible
     * 
     * @return Temps formaté "Xh Ymin"
     */
    public String getFormattedTime() {
        if (totalTimeSeconds == null || totalTimeSeconds == 0) {
            return "0 min";
        }

        int hours = totalTimeSeconds / 3600;
        int minutes = (totalTimeSeconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%dh %dmin", hours, minutes);
        } else {
            return String.format("%dmin", minutes);
        }
    }

    /**
     * Retourne les instructions de navigation étape par étape
     * 
     * @return Liste des instructions
     */
    public List<String> getNavigationInstructions() {
        List<String> instructions = new ArrayList<>();

        if (edges == null || edges.isEmpty()) {
            return instructions;
        }

        for (int i = 0; i < edges.size(); i++) {
            RoadEdge edge = edges.get(i);
            String instruction = String.format(
                    "%d. Suivez %s pendant %.2f km (%s)",
                    i + 1,
                    edge.getStreetName() != null ? edge.getStreetName() : "la route",
                    edge.getDistanceMetersOrCalculate() / 1000.0,
                    formatTime(edge.getTravelTimeSeconds()));
            instructions.add(instruction);
        }

        return instructions;
    }

    /**
     * Formate un temps en secondes en format lisible
     * 
     * @param seconds Temps en secondes
     * @return Temps formaté
     */
    private String formatTime(Integer seconds) {
        if (seconds == null || seconds == 0) {
            return "0 min";
        }

        int min = seconds / 60;
        if (min == 0) {
            return seconds + " sec";
        }
        return min + " min";
    }

    /**
     * Vérifie si la route est valide
     * 
     * @return true si la route contient au moins 2 nœuds
     */
    public boolean isValid() {
        return found != null && found &&
                nodes != null && nodes.size() >= 2 &&
                edges != null && edges.size() >= 1;
    }

    @Override
    public String toString() {
        if (!isValid()) {
            return "Route[invalid or not found]";
        }

        return String.format(
                "Route[%s → %s, distance=%.2f km, time=%s, segments=%d, explored=%d nodes]",
                startNode != null ? startNode.getName() : "?",
                endNode != null ? endNode.getName() : "?",
                totalDistanceKm,
                getFormattedTime(),
                getSegmentCount(),
                nodesExplored);
    }
}