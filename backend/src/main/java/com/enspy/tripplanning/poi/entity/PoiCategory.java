package com.enspy.tripplanning.poi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ================================================================
 * Entité PoiCategory - Catégorie de Points d'Intérêt
 * ================================================================
 * 
 * 🎯 OBJECTIFS:
 * - Classifier POI par type (Hébergement, Restaurant, etc.)
 * - Support hiérarchie (catégories parentes/enfants)
 * - Métadonnées visuelles (icône, couleur)
 * - Multilingue (FR + EN)
 * 
 * 🌳 HIÉRARCHIE:
 * Transport (parent)
 *   ├─ Station-service (enfant)
 *   ├─ Péage (enfant)
 *   └─ Gare (enfant)
 * 
 * 📊 RELATIONS:
 * - 1 Category → N POIs
 * - 1 Category → N Categories enfants (self-reference)
 * - 1 Category ← 1 Category parent (optionnel)
 * 
 * ================================================================
 * @author Planning Map Team
 * @version 1.0.0
 * @since 2024-12-14
 * ================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("poi_categories")
public class PoiCategory {

    // ============================================================
    // IDENTIFIANT & HIÉRARCHIE
    // ============================================================
    
    @Id
    @Column("category_id")
    private Long categoryId;
    
    /**
     * Catégorie parente (optionnel)
     * null = catégorie racine
     * non-null = sous-catégorie
     * 
     * 🌳 EXEMPLE:
     * Transport (parent_id=null)
     *   ├─ Station-service (parent_id=1)
     *   └─ Péage (parent_id=1)
     */
    @Column("parent_category_id")
    private Long parentCategoryId;

    // ============================================================
    // INFORMATIONS DE BASE
    // ============================================================
    
    /**
     * Nom catégorie (français)
     * Ex: "Hébergement", "Restaurant", "Station-service"
     * 
     * ⚠️ UNIQUE - pas de doublons
     */
    @Column("name")
    private String name;
    
    /**
     * Nom catégorie (anglais)
     * Ex: "Accommodation", "Restaurant", "Gas Station"
     * 
     * 🌍 Multilingue pour internationalisation
     */
    @Column("name_en")
    private String nameEn;
    
    /**
     * Slug URL-friendly
     * Ex: "hebergement", "station-service"
     * 
     * 🔗 UTILISÉ DANS:
     * - URLs: /pois/category/hebergement
     * - Frontend routing
     * - SEO
     * 
     * ⚠️ UNIQUE - généré automatiquement depuis name
     */
    @Column("slug")
    private String slug;
    
    /**
     * Description catégorie
     * Texte libre explicatif
     */
    @Column("description")
    private String description;

    // ============================================================
    // MÉTADONNÉES VISUELLES
    // ============================================================
    
    /**
     * Nom icône Material UI / Lucide
     * Ex: "hotel", "restaurant", "local_gas_station"
     * 
     * 🎨 FRONTEND:
     * <Icon name={category.icon} />
     */
    @Column("icon")
    private String icon;
    
    /**
     * Couleur hexadécimale (#RRGGBB)
     * Ex: "#3498DB" (bleu), "#E74C3C" (rouge)
     * 
     * 🎨 UTILISÉ POUR:
     * - Marqueurs carte
     * - Badges frontend
     * - Filtres visuels
     * 
     * ✅ Validé par constraint DB: CHAR(7)
     */
    @Column("color")
    private String color;

    // ============================================================
    // ORDRE & STATUT
    // ============================================================
    
    /**
     * Index ordre d'affichage
     * Plus petit = affiché en premier
     * 
     * 📊 EXEMPLE:
     * 0: Hébergement (prioritaire)
     * 1: Restaurant
     * 2: Station-service
     * ...
     */
    @Column("order_index")
    @Builder.Default
    private Integer orderIndex = 0;
    
    /**
     * Catégorie active/visible
     * false = masquée (soft delete)
     */
    @Column("is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ============================================================
    // TIMESTAMPS
    // ============================================================
    
    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * Auto-update par trigger PostgreSQL
     */
    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ============================================================
    // CHAMPS TRANSIENTS (Relations)
    // ============================================================
    
    /**
     * Catégorie parente (chargée si demandé)
     * null si catégorie racine
     */
    @Transient
    private PoiCategory parentCategory;
    
    /**
     * Catégories enfants (chargées si demandé)
     * Vide si catégorie feuille
     */
    @Transient
    private List<PoiCategory> childCategories;
    
    /**
     * Nombre de POI dans cette catégorie
     * Calculé dynamiquement (pas stocké)
     */
    @Transient
    private Long poiCount;

    // ============================================================
    // MÉTHODES UTILITAIRES
    // ============================================================
    
    /**
     * Vérifie si c'est une catégorie racine
     * 
     * @return true si pas de parent
     */
    public boolean isRootCategory() {
        return this.parentCategoryId == null;
    }
    
    /**
     * Vérifie si c'est une sous-catégorie
     * 
     * @return true si a un parent
     */
    public boolean isSubCategory() {
        return this.parentCategoryId != null;
    }
    
    /**
     * Génère un slug depuis le nom
     * 
     * 🔧 TRANSFORMATIONS:
     * - Minuscules
     * - Espaces → tirets
     * - Accents supprimés
     * - Caractères spéciaux supprimés
     * 
     * EXEMPLES:
     * "Hébergement" → "hebergement"
     * "Station-service" → "station-service"
     * "Aire de repos" → "aire-de-repos"
     * 
     * @return Slug généré
     */
    public String generateSlug() {
        if (this.name == null || this.name.isBlank()) {
            return "";
        }
        
        return this.name
            .toLowerCase()
            .trim()
            // Remplace accents
            .replaceAll("[éèêë]", "e")
            .replaceAll("[àâä]", "a")
            .replaceAll("[ùûü]", "u")
            .replaceAll("[ïî]", "i")
            .replaceAll("[ôö]", "o")
            .replaceAll("[ç]", "c")
            // Espaces → tirets
            .replaceAll("\\s+", "-")
            // Supprime caractères spéciaux (garde tirets)
            .replaceAll("[^a-z0-9-]", "")
            // Supprime tirets multiples
            .replaceAll("-+", "-")
            // Supprime tirets début/fin
            .replaceAll("^-|-$", "");
    }
    
    /**
     * Désactive la catégorie (soft delete)
     * Recommandé: désactiver aussi les POI associés
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * Réactive la catégorie
     */
    public void activate() {
        this.isActive = true;
    }
    
    /**
     * Retourne le nom selon la langue
     * 
     * @param language Code langue ("fr" ou "en")
     * @return Nom dans la langue demandée
     */
    public String getNameByLanguage(String language) {
        if ("en".equalsIgnoreCase(language) && this.nameEn != null) {
            return this.nameEn;
        }
        return this.name;
    }
    
    /**
     * Retourne le chemin complet hiérarchique
     * 
     * 📍 EXEMPLE:
     * Transport > Station-service
     * Hébergement > Hôtel > Hôtel de luxe
     * 
     * @return Chemin hiérarchique
     */
    public String getFullPath() {
        StringBuilder path = new StringBuilder(this.name);
        
        if (this.parentCategory != null) {
            path.insert(0, this.parentCategory.getFullPath() + " > ");
        }
        
        return path.toString();
    }
}