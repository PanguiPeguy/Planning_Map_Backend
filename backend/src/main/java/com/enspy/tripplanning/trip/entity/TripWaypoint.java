package com.enspy.tripplanning.trip.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ================================================================
 * Entité TripWaypoint - Étape d'un Voyage
 * ================================================================
 * 
 * 🎯 OBJECTIFS:
 * - Définir les étapes ordonnées d'un voyage
 * - Support POI existants ET coordonnées custom
 * - Planification temporelle (arrivée/départ)
 * - Suivi réalisation (actual vs planned)
 * 
 * 📍 TYPES DE WAYPOINTS:
 * - START: Point de départ
 * - WAYPOINT: Étape intermédiaire
 * - END: Destination finale
 * 
 * 🔄 ORDRE:
 * - order_index définit la séquence: 0, 1, 2, ...
 * - Permet réorganisation drag&drop
 * 
 * ================================================================
 * @author Planning Map Team
 * @version 1.0.0
 * @since 2024-12-15
 * ================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("trip_waypoints")
public class TripWaypoint {

    // ============================================================
    // IDENTIFIANT & RELATIONS
    // ============================================================
    
    @Id
    @Column("waypoint_id")
    private Long waypointId;
    
    /**
     * Voyage parent (FK → trips)
     */
    @Column("trip_id")
    private UUID tripId;
    
    /**
     * POI associé (optionnel)
     * null si point custom sans POI
     */
    @Column("poi_id")
    private Long poiId;
    
    /**
     * Ordre dans le voyage (0, 1, 2, ...)
     * Définit la séquence des étapes
     */
    @Column("order_index")
    private Integer orderIndex;
    
    /**
     * Type de waypoint
     */
    @Column("waypoint_type")
    @Builder.Default
    private WaypointType waypointType = WaypointType.WAYPOINT;

    // ============================================================
    // COORDONNÉES CUSTOM (si pas de POI)
    // ============================================================
    
    /**
     * Nom custom si pas de POI
     * Ex: "Chez Grand-mère", "Parking forêt"
     */
    @Column("custom_name")
    private String customName;
    
    /**
     * Latitude custom (si poi_id = null)
     */
    @Column("custom_latitude")
    private BigDecimal customLatitude;
    
    /**
     * Longitude custom (si poi_id = null)
     */
    @Column("custom_longitude")
    private BigDecimal customLongitude;

    // ============================================================
    // PLANIFICATION TEMPORELLE
    // ============================================================
    
    /**
     * Heure d'arrivée prévue
     */
    @Column("planned_arrival_time")
    private LocalDateTime plannedArrivalTime;
    
    /**
     * Heure de départ prévue
     */
    @Column("planned_departure_time")
    private LocalDateTime plannedDepartureTime;
    
    /**
     * Durée prévue d'arrêt (minutes)
     * Ex: 60min pour déjeuner, 480min pour nuit d'hôtel
     */
    @Column("planned_duration_minutes")
    private Integer plannedDurationMinutes;

    // ============================================================
    // SUIVI RÉALISATION
    // ============================================================
    
    /**
     * Heure d'arrivée réelle
     * Renseigné pendant le voyage
     */
    @Column("actual_arrival_time")
    private LocalDateTime actualArrivalTime;
    
    /**
     * Heure de départ réelle
     */
    @Column("actual_departure_time")
    private LocalDateTime actualDepartureTime;

    // ============================================================
    // NOTES & MÉTADONNÉES
    // ============================================================
    
    /**
     * Notes libres utilisateur
     * Ex: "Réserver table restaurant", "Prévoir parapluie"
     */
    @Column("notes")
    private String notes;

    // ============================================================
    // TIMESTAMPS
    // ============================================================
    
    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ============================================================
    // CHAMPS TRANSIENTS
    // ============================================================
    
    /**
     * POI complet (chargé si demandé)
     */
    @Transient
    private com.enspy.tripplanning.poi.entity.Poi poi;

    // ============================================================
    // MÉTHODES MÉTIER
    // ============================================================
    
    /**
     * Vérifie si c'est un point custom (sans POI)
     * 
     * @return true si coordinates custom
     */
    public boolean isCustomPoint() {
        return poiId == null;
    }
    
    /**
     * Retourne le nom à afficher
     * 
     * @return Nom POI ou nom custom
     */
    public String getDisplayName() {
        if (poi != null) {
            return poi.getName();
        }
        return customName != null ? customName : "Point " + orderIndex;
    }
    
    /**
     * Retourne latitude (POI ou custom)
     * 
     * @return Latitude
     */
    public BigDecimal getLatitude() {
        if (poi != null) {
            return poi.getLatitude();
        }
        return customLatitude;
    }
    
    /**
     * Retourne longitude (POI ou custom)
     * 
     * @return Longitude
     */
    public BigDecimal getLongitude() {
        if (poi != null) {
            return poi.getLongitude();
        }
        return customLongitude;
    }
    
    /**
     * Enregistre l'arrivée réelle
     */
    public void recordArrival() {
        this.actualArrivalTime = LocalDateTime.now();
    }
    
    /**
     * Enregistre le départ réel
     */
    public void recordDeparture() {
        this.actualDepartureTime = LocalDateTime.now();
    }
    
    /**
     * Calcule le retard/avance (minutes)
     * 
     * @return Minutes de retard (positif) ou avance (négatif)
     */
    public Integer getDelayMinutes() {
        if (plannedArrivalTime != null && actualArrivalTime != null) {
            return (int) java.time.Duration.between(
                plannedArrivalTime, 
                actualArrivalTime
            ).toMinutes();
        }
        return null;
    }

    // ============================================================
    // ENUM
    // ============================================================
    
    /**
     * Types de waypoint
     */
    public enum WaypointType {
        /** Point de départ */
        START,
        /** Étape intermédiaire */
        WAYPOINT,
        /** Destination finale */
        END
    }
}