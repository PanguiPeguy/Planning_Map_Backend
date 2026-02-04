package com.enspy.tripplanning.trip.entity;

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
 * ================================================================
 * Entité TripMember - Membre d'un Voyage
 * ================================================================
 * 
 * 🎯 OBJECTIFS:
 * - Gérer collaboration multi-utilisateurs
 * - Définir rôles et permissions
 * - Tracking activité membres
 * - Notifications personnalisées
 * 
 * 👥 RÔLES:
 * - OWNER: Propriétaire (tous droits)
 * - EDITOR: Peut modifier le voyage
 * - VIEWER: Lecture seule
 * 
 * 🔔 NOTIFICATIONS:
 * - Activables/désactivables par membre
 * - Alertes modifications, messages, etc.
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
@Table("trip_members")
public class TripMember {

    // ============================================================
    // IDENTIFIANT & RELATIONS
    // ============================================================
    
    /**
     * ID composite de la relation
     */
    @Id
    @Column("trip_member_id")
    private UUID tripMemberId;
    
    /**
     * ID du voyage (FK → trips)
     */
    @Column("trip_id")
    private UUID tripId;
    
    /**
     * ID de l'utilisateur membre (FK → users)
     */
    @Column("user_id")
    private UUID userId;

    // ============================================================
    // RÔLE & PERMISSIONS
    // ============================================================
    
    /**
     * Rôle du membre dans le voyage
     */
    @Column("role")
    @Builder.Default
    private MemberRole role = MemberRole.VIEWER;

    // ============================================================
    // ACTIVITÉ & NOTIFICATIONS
    // ============================================================
    
    /**
     * Date d'ajout au voyage
     */
    @Column("joined_at")
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    /**
     * Dernière activité du membre
     * Mise à jour à chaque action
     */
    @Column("last_activity_at")
    private LocalDateTime lastActivityAt;
    
    /**
     * Notifications activées pour ce voyage
     */
    @Column("notifications_enabled")
    @Builder.Default
    private Boolean notificationsEnabled = true;

    // ============================================================
    // CHAMPS TRANSIENTS
    // ============================================================
    
    /**
     * Informations complètes de l'utilisateur (chargé si demandé)
     */
    @Transient
    private com.enspy.tripplanning.authentification.entity.User user;
    
    /**
     * Voyage associé (chargé si demandé)
     */
    @Transient
    private Trip trip;

    // ============================================================
    // MÉTHODES MÉTIER
    // ============================================================
    
    /**
     * Vérifie si le membre peut modifier le voyage
     * 
     * @return true si OWNER ou EDITOR
     */
    public boolean canEdit() {
        return role == MemberRole.OWNER || role == MemberRole.EDITOR;
    }
    
    /**
     * Vérifie si le membre est propriétaire
     * 
     * @return true si OWNER
     */
    public boolean isOwner() {
        return role == MemberRole.OWNER;
    }
    
    /**
     * Vérifie si le membre peut seulement lire
     * 
     * @return true si VIEWER
     */
    public boolean isViewerOnly() {
        return role == MemberRole.VIEWER;
    }
    
    /**
     * Met à jour la dernière activité
     * Appelé à chaque action du membre
     */
    public void recordActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    
    /**
     * Active les notifications
     */
    public void enableNotifications() {
        this.notificationsEnabled = true;
    }
    
    /**
     * Désactive les notifications
     */
    public void disableNotifications() {
        this.notificationsEnabled = false;
    }
    
    /**
     * Promeut le membre à EDITOR
     */
    public void promoteToEditor() {
        if (this.role == MemberRole.VIEWER) {
            this.role = MemberRole.EDITOR;
        }
    }
    
    /**
     * Rétrograde le membre à VIEWER
     */
    public void demoteToViewer() {
        if (this.role == MemberRole.EDITOR) {
            this.role = MemberRole.VIEWER;
        }
    }

    // ============================================================
    // ENUM
    // ============================================================
    
    /**
     * Rôles possibles pour un membre de voyage
     */
    public enum MemberRole {
        /** Propriétaire - Contrôle total */
        OWNER,
        /** Éditeur - Peut modifier */
        EDITOR,
        /** Lecteur - Lecture seule */
        VIEWER
    }
}