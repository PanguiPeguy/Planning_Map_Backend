package com.enspy.tripplanning.itinerary.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité Itinéraire
 * Représente un trajet enregistré/modèle
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("itineraries")
public class Itinerary implements org.springframework.data.domain.Persistable<UUID> {
    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("user_id")
    private UUID userId;

    @Column("origin_location")
    private String originLocation; // Nom ou coordonnées JSON

    @Column("destination_location")
    private String destinationLocation; // Nom ou coordonnées JSON

    @Column("waypoints_json")
    private String waypointsJson; // Liste des POIs ou points intermédiaires (JSON array)

    @Column("geometry_encoded")
    private String geometryEncoded; // Polyline encodée du tracé

    @Column("distance_meters")
    private Double distanceMeters;

    @Column("duration_seconds")
    private Integer durationSeconds;

    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Transient
    private boolean isNew = false; // Pour R2DBC/JDBC pour forcer l'INSERT si ID présent

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }
}
