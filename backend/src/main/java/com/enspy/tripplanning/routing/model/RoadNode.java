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
 * Représente un nœud dans le graphe routier.
 * 
 * Un nœud peut être :
 * - Une intersection de routes
 * - Un point de référence (POI)
 * - Un point de jonction important
 * 
 * Selon la modélisation mathématique : V = ensemble des nœuds du graphe G=(V,E,w)
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2024-12-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("road_nodes")
public class RoadNode {

    /**
     * Identifiant unique du nœud
     */
    @Id
    @Column("node_id")
    private Long nodeId;

    /**
     * Identifiant OpenStreetMap pour traçabilité
     * Permet de relier le nœud aux données OSM sources
     */
    @Column("osm_id")
    private Long osmId;

    /**
     * Latitude du nœud (coordonnée GPS)
     * Range: [-90, 90]
     */
    @Column("latitude")
    private Double latitude;

    /**
     * Longitude du nœud (coordonnée GPS)
     * Range: [-180, 180]
     */
    @Column("longitude")
    private Double longitude;

    /**
     * Type de nœud dans le graphe
     * Valeurs possibles:
     * - "intersection" : croisement de routes
     * - "poi_reference" : point d'intérêt
     * - "junction" : jonction autoroutière
     * - "roundabout" : rond-point
     */
    @Column("node_type")
    private String nodeType;

    /**
     * Référence optionnelle à un Point d'Intérêt
     * Null si le nœud n'est pas associé à un POI
     */
    @Column("poi_id")
    private Long poiId;

    /**
     * Nom du nœud (si applicable)
     * Exemple: "Carrefour Bastos", "Rond-Point Deido"
     */
    @Column("name")
    private String name;

    /**
     * Métadonnées additionnelles au format JSON
     * Stocke les attributs OSM bruts pour flexibilité
     * Exemple: {"highway": "traffic_signals", "ref": "N1"}
     */
    @Column("tags")
    private String tagsJson;

    /**
     * Date de création du nœud
     */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * Date de dernière mise à jour
     */
    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Géométrie PostGIS (non mappée par R2DBC)
     * Utilisée uniquement par les requêtes SQL directes
     */
    @Transient
    private String geom;

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    /**
     * Calcule la distance euclidienne entre ce nœud et un autre
     * Utilisé pour l'heuristique de l'algorithme A*
     * 
     * Formule: distance = R⊕ × acos(sin(lat1)×sin(lat2) + cos(lat1)×cos(lat2)×cos(lon2-lon1))
     * où R⊕ ≈ 6371 km (rayon de la Terre)
     * 
     * @param other Autre nœud
     * @return Distance en kilomètres
     */
    public double distanceTo(RoadNode other) {
        if (other == null) {
            throw new IllegalArgumentException("Le nœud cible ne peut pas être null");
        }

        final double EARTH_RADIUS_KM = 6371.0;

        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double lon1Rad = Math.toRadians(this.longitude);
        double lon2Rad = Math.toRadians(other.longitude);

        double angle = Math.acos(
            Math.sin(lat1Rad) * Math.sin(lat2Rad) +
            Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.cos(lon2Rad - lon1Rad)
        );

        return EARTH_RADIUS_KM * angle;
    }

    /**
     * Vérifie si ce nœud est lié à un POI
     * 
     * @return true si le nœud référence un POI
     */
    public boolean isPOINode() {
        return this.poiId != null;
    }

    /**
     * Vérifie si ce nœud est une intersection
     * 
     * @return true si le type est "intersection"
     */
    public boolean isIntersection() {
        return "intersection".equalsIgnoreCase(this.nodeType);
    }

    /**
     * Retourne une représentation textuelle du nœud
     * Format: "Node[id=1, name=Carrefour Bastos, lat=3.8667, lon=11.5167]"
     */
    @Override
    public String toString() {
        return String.format("RoadNode[id=%d, name=%s, lat=%.4f, lon=%.4f, type=%s]",
            nodeId, name, latitude, longitude, nodeType);
    }
}