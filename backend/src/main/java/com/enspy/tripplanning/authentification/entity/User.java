package com.enspy.tripplanning.authentification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * ================================================================
 * Entité User - Utilisateur du système
 * ================================================================
 * 
 * 🎯 OBJECTIFS:
 * - Authentification JWT sécurisée
 * - Support rôles ADMIN/USER
 * - Profil complet utilisateur
 * - Tracking connexions
 * 
 * 🔐 SÉCURITÉ:
 * - Password BCrypt hashé
 * - Email verification
 * - Reset password flow
 * - Session tracking
 * 
 * 📊 RELATIONS:
 * - 1 User → N POIs créés
 * - 1 User → N Trips possédés
 * - 1 User → N Reviews écrits
 * - 1 User → N POIs favoris
 * 
 * ================================================================
 * @author Votre équipe Planning Map
 * @version 1.0.0
 * @since 2024-12-14
 * ================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User implements UserDetails {

    // ============================================================
    // IDENTIFIANT & AUTHENTIFICATION
    // ============================================================
    
    @Id
    @Column("user_id")
    private UUID userId;
    
    /**
     * Nom d'utilisateur unique (3-50 caractères)
     * Utilisé pour affichage, pas pour connexion
     */
    @Column("username")
    private String username;
    
    /**
     * Email unique - Identifiant de connexion principal
     * Format validé par constraint DB
     */
    @Column("email")
    private String email;
    
    /**
     * Hash BCrypt du mot de passe (60+ caractères)
     * ⚠️ JAMAIS exposer dans les API responses
     */
    @Column("password_hash")
    private String passwordHash;
    
    /**
     * Rôle utilisateur: ADMIN ou USER
     * Définit les permissions système
     */
    @Column("role")
    @Builder.Default
    private UserRole role = UserRole.USER;
    
    // ============================================================
    // PROFIL UTILISATEUR
    // ============================================================
    
    /**
     * Nom entreprise (pour USERs professionnels)
     * Optionnel - affiché dans interface
     */
    @Column("company_name")
    private String companyName;
    
    /**
     * Téléphone avec indicatif international
     * Format: +237XXXXXXXXX
     */
    @Column("phone")
    private String phone;
    
    /**
     * Ville de résidence
     * Utilisé pour recommandations POI locaux
     */
    @Column("city")
    private String city;
    
    /**
     * Pays (défaut: Cameroun)
     */
    @Column("transportmode")
    private String transportmode;
    
    /**
     * URL photo de profil (Cloudinary/S3)
     * Optionnel - null si pas de photo
     */
    @Column("profile_photo_url")
    private String profilePhotoUrl;
    
    // ============================================================
    // STATUT COMPTE
    // ============================================================
    
    /**
     * Compte activé/désactivé
     * false = compte suspendu par admin
     */
    @Column("is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * Email vérifié via token
     * false jusqu'à clic lien confirmation
     */
    @Column("is_verified")
    @Builder.Default
    private Boolean isVerified = false;
    
    /**
     * Token unique pour vérification email
     * Généré à l'inscription, supprimé après validation
     */
    @Column("verification_token")
    private String verificationToken;
    
    /**
     * Token unique pour réinitialisation mot de passe
     * Temporaire (15 minutes de validité)
     */
    @Column("reset_password_token")
    private String resetPasswordToken;
    
    /**
     * Date expiration token reset password
     * Après cette date, token invalide
     */
    @Column("reset_password_expires")
    private LocalDateTime resetPasswordExpires;
    
    // ============================================================
    // MÉTADONNÉES CONNEXION
    // ============================================================
    
    /**
     * Date/heure dernière connexion réussie
     * Utilisé pour statistiques utilisateur
     */
    @Column("last_login_at")
    private LocalDateTime lastLoginAt;
    
    /**
     * Adresse IP dernière connexion
     * Sécurité: détection connexions suspectes
     */
    @Column("last_login_ip")
    private String lastLoginIp;
    
    /**
     * Nombre total de connexions
     * Incrémenté à chaque login réussi
     */
    @Column("login_count")
    @Builder.Default
    private Integer loginCount = 0;
    
    // ============================================================
    // TIMESTAMPS (AUTO-GÉRÉS)
    // ============================================================
    
    /**
     * Date création compte
     * ⚠️ Immutable - ne jamais modifier
     */
    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * Date dernière modification profil
     * 🔄 Auto-update via trigger PostgreSQL
     */
    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // ============================================================
    // CHAMPS TRANSIENTS (non stockés DB)
    // ============================================================
    
    /**
     * Statistiques utilisateur (chargées à la demande)
     * Non stocké en DB - calculé dynamiquement
     */
    @Transient
    private UserStatistics statistics;
    
    // ============================================================
    // IMPLÉMENTATION UserDetails (Spring Security)
    // ============================================================
    
    /**
     * Retourne les autorités (rôles) de l'utilisateur
     * 
     * 🔐 MAPPING RÔLES → AUTHORITIES:
     * - USER → ROLE_USER
     * - ADMIN → ROLE_ADMIN
     * 
     * Utilisé par Spring Security pour vérifier permissions
     * 
     * @return Collection des autorités (singleton)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }
    
    /**
     * Retourne le mot de passe hashé
     * ⚠️ Utilisé uniquement par Spring Security
     * Ne JAMAIS exposer via API
     */
    @Override
    public String getPassword() {
        return this.passwordHash;
    }
    
    /**
     * Retourne l'identifiant de connexion (email)
     * 
     * 📧 IMPORTANT: On utilise EMAIL comme username
     * Pas le champ 'username' qui est juste un pseudonyme
     */
    @Override
    public String getUsername() {
        return this.email;
    }
    
    /**
     * Compte non expiré ?
     * Toujours true dans notre système
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    /**
     * Compte non verrouillé ?
     * Basé sur isActive
     */
    @Override
    public boolean isAccountNonLocked() {
        return this.isActive;
    }
    
    /**
     * Credentials non expirées ?
     * Toujours true (pas de politique rotation mdp)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    /**
     * Compte activé ?
     * Basé sur isActive
     */
    @Override
    public boolean isEnabled() {
        return this.isActive;
    }
    
    // ============================================================
    // MÉTHODES UTILITAIRES
    // ============================================================
    
    /**
     * Vérifie si l'utilisateur est administrateur
     * 
     * @return true si ADMIN, false sinon
     */
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
    
    /**
     * Vérifie si l'utilisateur est USER
     * 
     * @return true si USER, false sinon
     */
    public boolean isUSER() {
        return this.role == UserRole.USER;
    }
    
    /**
     * Retourne le nom d'affichage de l'utilisateur
     * Priorité: company_name > username > email
     * 
     * @return Nom à afficher dans l'interface
     */
    public String getDisplayName() {
        if (companyName != null && !companyName.isBlank()) {
            return companyName;
        }
        if (username != null && !username.isBlank()) {
            return username;
        }
        return email;
    }
    
    /**
     * Enregistre une nouvelle connexion
     * Met à jour lastLoginAt, lastLoginIp, loginCount
     * 
     * @param ipAddress Adresse IP de la connexion
     */
    public void recordLogin(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.loginCount = (this.loginCount != null ? this.loginCount : 0) + 1;
    }
    
    /**
     * Génère un token de vérification email
     * UUID aléatoire sécurisé
     * 
     * @return Token généré
     */
    public String generateVerificationToken() {
        this.verificationToken = UUID.randomUUID().toString();
        return this.verificationToken;
    }
    
    /**
     * Génère un token de reset password avec expiration
     * Valide 15 minutes
     * 
     * @return Token généré
     */
    public String generateResetPasswordToken() {
        this.resetPasswordToken = UUID.randomUUID().toString();
        this.resetPasswordExpires = LocalDateTime.now().plusMinutes(15);
        return this.resetPasswordToken;
    }
    
    /**
     * Vérifie si le token reset password est encore valide
     * 
     * @return true si valide, false si expiré ou null
     */
    public boolean isResetPasswordTokenValid() {
        return this.resetPasswordToken != null 
            && this.resetPasswordExpires != null
            && this.resetPasswordExpires.isAfter(LocalDateTime.now());
    }
    
    /**
     * Valide l'email et nettoie le token
     */
    public void verifyEmail() {
        this.isVerified = true;
        this.verificationToken = null;
    }
    
    /**
     * Réinitialise le mot de passe et nettoie les tokens
     * 
     * @param newPasswordHash Nouveau hash BCrypt
     */
    public void resetPassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.resetPasswordToken = null;
        this.resetPasswordExpires = null;
    }
    
    // ============================================================
    // CLASSES INTERNES
    // ============================================================
    
    /**
     * Enum rôles utilisateur
     * Correspond au type ENUM en PostgreSQL
     */
    public enum UserRole {
        /** Administrateur - Accès complet système */
        ADMIN,
        /** Utilisateur standard */
        USER
    }
    
    /**
     * DTO Statistiques utilisateur
     * Calculé dynamiquement, non stocké
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatistics {
        /** Nombre de POI créés */
        private Long poisCreated;
        /** Nombre de voyages possédés */
        private Long tripsOwned;
        /** Nombre d'avis écrits */
        private Long reviewsWritten;
        /** Nombre de POI en favoris */
        private Long poisFavorited;
    }
}