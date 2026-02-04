package com.enspy.tripplanning.trip.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * Entité Trip - Voyage Planifié (Collaboratif)
 * ================================================================
 * 
 * 🎯 OBJECTIFS:
 * - Planification voyages multi-étapes
 * - Collaboration temps réel (owner + membres)
 * - Calcul itinéraires optimisés
 * - Partage via lien unique
 * - Statistiques voyage (distance, durée, coût)
 * 
 * 👥 COLLABORATION:
 * - 1 Owner (créateur)
 * - N Editors (peuvent modifier)
 * - N Viewers (lecture seule)
 * - Partage public optionnel
 * 
 * 📊 RELATIONS:
 * - 1 Trip → 1 User (owner)
 * - 1 Trip → N TripMembers
 * - 1 Trip → N TripWaypoints (ordonnés)
 * - 1 Trip → N CalculatedRoutes (segments)
 * 
 * ================================================================
 * 
 * @author Planning Map Team
 * @version 1.0.0
 * @since 2024-12-15
 *        ================================================================
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("trips")
public class Trip {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    // ============================================================
    // IDENTIFIANT & PROPRIÉTAIRE
    // ============================================================

    @Id
    @Column("trip_id")
    private UUID tripId;

    @Column("owner_id")
    private UUID ownerUserId;

    // ============================================================
    // INFORMATIONS DE BASE
    // ============================================================

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("start_date")
    private LocalDate startDate;

    @Column("end_date")
    private LocalDate endDate;

    // ============================================================
    // STATUT VOYAGE
    // ============================================================

    @Column("status")
    @Builder.Default
    private TripStatus status = TripStatus.DRAFT;

    // ============================================================
    // COLLABORATION & PARTAGE
    // ============================================================

    @Column("is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column("is_collaborative")
    @Builder.Default
    private Boolean isCollaborative = false;

    @Column("share_token")
    private String shareToken;

    // ============================================================
    // STATISTIQUES CALCULÉES
    // ============================================================

    @Column("total_distance_km")
    private BigDecimal totalDistanceKm;

    @Column("total_duration_minutes")
    private Integer totalDurationMinutes;

    @Column("estimated_cost")
    private BigDecimal estimatedCost;

    @Column("metadata")
    private String metadataJson;

    // ============================================================
    // TIMESTAMPS
    // ============================================================

    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column("completed_at")
    private LocalDateTime completedAt;

    // ============================================================
    // CHAMPS TRANSIENTS
    // ============================================================

    @Transient
    private com.enspy.tripplanning.authentification.entity.User owner;

    @Transient
    private List<TripMember> members;

    @Transient
    private List<TripWaypoint> waypoints;

    @Transient
    private Map<String, Object> metadata;

    // ============================================================
    // MÉTHODES SÉRIALISATION JSONB
    // ============================================================

    public void serializeMetadata() {
        if (this.metadata != null) {
            try {
                this.metadataJson = JSON_MAPPER.writeValueAsString(this.metadata);
            } catch (JsonProcessingException e) {
                log.error("Erreur sérialisation metadata trip", e);
                this.metadataJson = "{}";
            }
        }
    }

    public void deserializeMetadata() {
        if (this.metadataJson != null && !this.metadataJson.isBlank()) {
            try {
                this.metadata = JSON_MAPPER.readValue(
                        this.metadataJson,
                        new TypeReference<Map<String, Object>>() {
                        });
            } catch (JsonProcessingException e) {
                log.error("Erreur désérialisation metadata trip", e);
                this.metadata = new HashMap<>();
            }
        }
    }

    // ============================================================
    // MÉTHODES MÉTIER
    // ============================================================

    public String generateShareToken() {
        this.shareToken = UUID.randomUUID().toString().replace("-", "");
        return this.shareToken;
    }

    public void revokeShareToken() {
        this.shareToken = null;
    }

    public boolean isOwner(UUID userId) {
        return this.ownerUserId.equals(userId);
    }

    public void start() {
        this.status = TripStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = TripStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TripStatus.CANCELLED;
    }

    public Long getDurationDays() {
        if (this.startDate != null && this.endDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(this.startDate, this.endDate) + 1;
        }
        return null;
    }

    public String getFormattedDuration() {
        if (this.totalDurationMinutes == null) {
            return "Non calculé";
        }

        int hours = this.totalDurationMinutes / 60;
        int minutes = this.totalDurationMinutes % 60;

        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "min";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return minutes + "min";
        }
    }

    // ============================================================
    // ENUMS
    // ============================================================

    public enum TripStatus {
        DRAFT, PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}