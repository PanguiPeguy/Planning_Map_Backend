package com.enspy.tripplanning.routing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Représente une arête (route) dans le graphe routier.
 * 
 * Une arête connecte deux nœuds et possède un poids représentant
 * le temps de parcours.
 * 
 * Selon la modélisation mathématique :
 * - E = ensemble des arêtes du graphe G=(V,E,w)
 * - w(u,v) = poids de l'arête = temps de parcours
 * - w(u,v) = distance / vitesse_max
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2024-12-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("road_edges")
public class RoadEdge {

    /**
     * Identifiant unique de l'arête
     */
    @Id
    @Column("edge_id")
    private Long edgeId;

    /**
     * Identifiant OpenStreetMap de la route
     */
    @Column("osm_way_id")
    private Long osmWayId;

    /**
     * ID du nœud source (départ)
     */
    @Column("source_node_id")
    private Long sourceNodeId;

    /**
     * ID du nœud cible (arrivée)
     */
    @Column("target_node_id")
    private Long targetNodeId;

    @Column("distance_km")
    private Double distanceKm;

    /**
     * Distance de la route en mètres
     * Utilisé pour calculer le temps de parcours
     */
    @Column("distance_meters")
    private Double distanceMeters;

    /**
     * Type de route selon classification OSM
     * 
     * Hiérarchie des routes (du plus rapide au plus lent):
     * - "motorway" : Autoroute (130 km/h)
     * - "trunk" : Route express (110 km/h)
     * - "primary" : Route nationale (90 km/h)
     * - "secondary" : Route départementale (70 km/h)
     * - "tertiary" : Route tertiaire (50 km/h)
     * - "residential" : Rue résidentielle (30 km/h)
     * - "service" : Voie de service (20 km/h)
     */
    @Column("road_type")
    private String roadType;

    /**
     * Vitesse maximale autorisée sur la route (km/h)
     * Détermine le temps de parcours théorique
     */
    @Column("max_speed_kmh")
    private Integer maxSpeedKmh;

    /**
     * Temps de parcours en secondes
     * 
     * Calculé par: travel_time = (distance_meters / 1000) / max_speed_kmh * 3600
     * 
     * C'est le poids w(u,v) utilisé par l'algorithme A*
     */
    @Column("travel_time_seconds")
    private Integer travelTimeSeconds;

    /**
     * Indique si la route est à sens unique
     * - true : on ne peut circuler que de source → target
     * - false : circulation bidirectionnelle
     */
    @Column("one_way")
    private Boolean oneWay;

    /**
     * Nom de la rue
     * Exemple: "Avenue Kennedy", "Boulevard du 20 Mai"
     */
    @Column("road_name")
    private String streetName;

    /**
     * Métadonnées OSM au format JSON
     * Exemple: {"surface": "asphalt", "lanes": 2, "lit": "yes"}
     */
    @Column("tags")
    private String tagsJson;

    /**
     * Date de création
     */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * Date de dernière mise à jour
     */
    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Géométrie PostGIS LineString (non mappée par R2DBC)
     * Représente le tracé réel de la route
     */
    @Transient
    private String geom;

    /**
     * Nœud source (chargé dynamiquement)
     */
    @Transient
    private RoadNode sourceNode;

    /**
     * Nœud cible (chargé dynamiquement)
     */
    @Transient
    private RoadNode targetNode;

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    /**
     * Calcule le temps de parcours théorique en secondes
     * 
     * Formule: temps = (distance_km / vitesse_kmh) × 3600
     * 
     * @return Temps en secondes
     */
    public int calculateTravelTime() {
        if (distanceMeters == null || maxSpeedKmh == null || maxSpeedKmh == 0) {
            return 0;
        }

        double distanceKm = distanceMeters / 1000.0;
        return (int) Math.ceil((distanceKm / maxSpeedKmh) * 3600);
    }

    /**
     * Retourne le poids de l'arête pour l'algorithme A*
     * 
     * Dans notre cas: w(u,v) = travel_time_seconds
     * 
     * @return Poids de l'arête
     */
    public int getWeight() {
        return this.travelTimeSeconds != null ? this.travelTimeSeconds : 0;
    }

    /**
     * Vérifie si cette arête est praticable en voiture
     * Exclut les routes piétonnes, pistes cyclables, etc.
     * 
     * @return true si praticable en voiture
     */
    public boolean isDrivable() {
        if (roadType == null)
            return false;

        return !roadType.equalsIgnoreCase("footway") &&
                !roadType.equalsIgnoreCase("cycleway") &&
                !roadType.equalsIgnoreCase("path") &&
                !roadType.equalsIgnoreCase("steps");
    }

    /**
     * Retourne la vitesse moyenne réaliste (80% de la vitesse max)
     * Prend en compte les conditions réelles de circulation
     * 
     * @return Vitesse moyenne en km/h
     */
    public int getRealisticSpeed() {
        return maxSpeedKmh != null ? (int) (maxSpeedKmh * 0.8) : 0;
    }

    /**
     * Vérifie si cette route est une autoroute ou voie rapide
     * 
     * @return true si autoroute ou trunk
     */
    public boolean isHighway() {
        return "motorway".equalsIgnoreCase(roadType) ||
                "trunk".equalsIgnoreCase(roadType);
    }

    @Override
    public String toString() {
        return String.format("RoadEdge[id=%d, from=%d, to=%d, distance=%.2fm, type=%s, time=%ds]",
                edgeId, sourceNodeId, targetNodeId, distanceMeters, roadType, travelTimeSeconds);
    }
}