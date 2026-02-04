package com.enspy.tripplanning.poi.entity;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * Entité Review - Avis utilisateur sur POI
 * ================================================================
 * 
 * 🎯 OBJECTIFS:
 * - Permettre avis authentiques utilisateurs
 * - Système notation 0-5 étoiles
 * - Support photos utilisateur
 * - Vérification visite (GPS optional)
 * - Modération contenu
 * 
 * 📊 RELATIONS:
 * - N Reviews → 1 POI
 * - N Reviews → 1 User (auteur)
 * - 1 Review → 1 POI (UNIQUE constraint user+poi)
 * 
 * 🔐 CONTRAINTES:
 * - 1 avis max par user par POI
 * - Rating 0.0 à 5.0 obligatoire
 * - Modifiable par auteur seulement
 * 
 * ================================================================
 * 
 * @author Planning Map Team
 * @version 1.0.0
 * @since 2024-12-14
 *        ================================================================
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("poi_reviews")
public class Review {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    // ============================================================
    // IDENTIFIANT & RELATIONS
    // ============================================================

    @Id
    @Column("review_id")
    private Long reviewId;

    /**
     * POI concerné (FK → pois)
     * ⚠️ CASCADE DELETE: si POI supprimé, reviews aussi
     */
    @Column("poi_id")
    private Long poiId;

    /**
     * Auteur de l'avis (FK → users)
     * ⚠️ CASCADE DELETE: si user supprimé, reviews aussi
     */
    @Column("user_id")
    private UUID userId;

    // ============================================================
    // CONTENU AVIS
    // ============================================================

    /**
     * Note attribuée (0.0 à 5.0)
     * 
     * 📊 ÉCHELLE:
     * 0.0-1.0: Très mauvais
     * 1.0-2.0: Mauvais
     * 2.0-3.0: Moyen
     * 3.0-4.0: Bon
     * 4.0-5.0: Excellent
     * 
     * ⚠️ Validé par constraint DB
     */
    @Column("rating")
    private BigDecimal rating;

    /**
     * Commentaire textuel (optionnel)
     * Peut être null si juste notation
     * 
     * 📝 RECOMMANDATIONS:
     * - Min 10 caractères (qualité)
     * - Max 1000 caractères (lisibilité)
     * - Modération offensive language
     */
    @Column("comment")
    private String comment;

    /**
     * Photos ajoutées par utilisateur (TEXT[] array)
     */
    @Column("images")
    private String[] images;

    @Column("is_verified_visit")
    @Builder.Default
    private Boolean isVerifiedVisit = false;

    /**
     * Avis modéré/approuvé par admin
     */
    @Column("is_moderated")
    private Boolean isModerated;

    /**
     * Signalé par autres utilisateurs
     */
    @Column("report_count")
    @Builder.Default
    private Integer reportCount = 0;

    /**
     * Nombre "utile" (upvotes)
     * Utilisateurs trouvent avis utile
     */
    @Column("helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

    // ============================================================
    // TIMESTAMPS
    // ============================================================

    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Auto-update par trigger PostgreSQL
     * Modifié si user édite avis
     */
    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * POI complet (chargé si demandé)
     */
    @Transient
    private Poi poi;

    /**
     * Auteur complet (chargé si demandé)
     * ⚠️ Masquer password_hash !
     */
    @Transient
    private com.enspy.tripplanning.authentification.entity.User author;

    /**
     * User connecté a trouvé avis utile
     * Calculé dynamiquement (non stocké)
     */
    @Transient
    private Boolean isHelpfulByCurrentUser;

    // Méthodes de sérialisation JSON supprimées car images est maintenant String[]
    // mapped to TEXT[]

    // ============================================================
    // MÉTHODES MÉTIER
    // ============================================================

    /**
     * Vérifie si l'avis est éditable
     * 
     * 📝 RÈGLES:
     * - Éditable dans 24h après création
     * - Pas éditable si modéré
     * 
     * @return true si éditable
     */
    public boolean isEditable() {
        LocalDateTime editDeadline = this.createdAt.plusHours(24);
        return LocalDateTime.now().isBefore(editDeadline)
                && (this.isModerated == null || !this.isModerated);
    }

    /**
     * Incrémente compteur "utile"
     */
    public void incrementHelpful() {
        this.helpfulCount = (this.helpfulCount != null ? this.helpfulCount : 0) + 1;
    }

    /**
     * Décrémente compteur "utile"
     */
    public void decrementHelpful() {
        this.helpfulCount = Math.max(0, (this.helpfulCount != null ? this.helpfulCount : 0) - 1);
    }

    /**
     * Signale l'avis (spam, offensive, etc.)
     */
    public void report() {
        this.reportCount = (this.reportCount != null ? this.reportCount : 0) + 1;
    }

    /**
     * Vérifie si l'avis doit être masqué automatiquement
     * 
     * @return true si >= 3 signalements
     */
    public boolean shouldBeHidden() {
        return this.reportCount != null && this.reportCount >= 3;
    }

    /**
     * Approuve l'avis (modération admin)
     */
    public void approve() {
        this.isModerated = true;
    }

    /**
     * Rejette l'avis (modération admin)
     */
    public void reject() {
        this.isModerated = false;
    }

    /**
     * Marque visite comme vérifiée (GPS)
     * 
     * @param verified true si vérifié
     */
    public void setVisitVerified(boolean verified) {
        this.isVerifiedVisit = verified;
    }

    /**
     * Retourne le rating en nombre d'étoiles (0-5)
     * 
     * @return Nombre entier d'étoiles
     */
    public int getStarRating() {
        if (this.rating == null) {
            return 0;
        }
        return this.rating.intValue();
    }

    /**
     * Calcule le temps écoulé depuis création
     * 
     * @return Texte formaté ("il y a 2 heures", "il y a 3 jours")
     */
    public String getTimeAgo() {
        LocalDateTime now = LocalDateTime.now();
        long seconds = java.time.Duration.between(this.createdAt, now).getSeconds();

        if (seconds < 60) {
            return "à l'instant";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return "il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return "il y a " + hours + " heure" + (hours > 1 ? "s" : "");
        } else if (seconds < 2592000) {
            long days = seconds / 86400;
            return "il y a " + days + " jour" + (days > 1 ? "s" : "");
        } else if (seconds < 31536000) {
            long months = seconds / 2592000;
            return "il y a " + months + " mois";
        } else {
            long years = seconds / 31536000;
            return "il y a " + years + " an" + (years > 1 ? "s" : "");
        }
    }

    /**
     * Vérifie si l'avis a des photos
     * 
     * @return true si images présentes
     */
    public boolean hasImages() {
        return this.images != null && this.images.length > 0;
    }

    /**
     * Compte le nombre de photos
     * 
     * @return Nombre de photos
     */
    public int getImageCount() {
        return this.images != null ? this.images.length : 0;
    }
}