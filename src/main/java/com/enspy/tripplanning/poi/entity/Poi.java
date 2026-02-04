package com.enspy.tripplanning.poi.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * Entité POI (Point Of Interest) - Point d'Intérêt
 * ================================================================
 * 
 * 🎯 OBJECTIFS:
 * - Stockage lieux géolocalisés (restaurants, hôtels, etc.)
 * - Support géométrie PostGIS (performances x50)
 * - Métadonnées flexibles (JSONB)
 * - Recherche full-text optimisée
 * 
 * 📍 GÉOLOCALISATION:
 * - Latitude/Longitude décimales
 * - Géométrie PostGIS auto-générée (trigger)
 * - Recherche spatiale rapide (index GIST)
 * 
 * 📊 RELATIONS:
 * - N POIs → 1 Category
 * - 1 POI → N Reviews
 * - N Users → M POIs (favoris)
 * - 1 POI ← 1 User (créateur optionnel)
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
@Table("pois")
public class Poi {

    // ============================================================
    // IDENTIFIANT & RELATIONS
    // ============================================================

    @Id
    @Column("poi_id")
    private Long poiId;

    /**
     * Catégorie du POI (FK → poi_categories)
     * Ex: Hébergement, Restaurant, Station-service
     */
    @Column("category_id")
    private Long categoryId;

    /**
     * Utilisateur créateur (optionnel)
     * null = POI créé par admin/import OSM
     * non-null = POI créé par client
     */
    @Column("created_by")
    private UUID createdByUserId;

    // ============================================================
    // INFORMATIONS DE BASE
    // ============================================================

    /**
     * Nom du POI
     * Ex: "Hotel Hilton Yaoundé", "Restaurant Le Biniou"
     * 
     * 🔍 Indexé pour recherche full-text
     */
    @Column("name")
    private String name;

    /**
     * Description détaillée
     * Texte libre, peut être long
     */
    @Column("description")
    private String description;

    /**
     * Type de POI
     * Ex: restaurant, hotel, gas_station, museum
     * 
     * 📌 Permet filtrage rapide avant catégorie
     */
    @Column("type")
    private String type;

    // ============================================================
    // GÉOLOCALISATION (CRUCIAL pour routing!)
    // ============================================================

    /**
     * Latitude GPS (-90 à +90)
     * Ex: 3.8667 pour Yaoundé
     * 
     * ⚠️ Validation constraint DB
     */
    @Column("latitude")
    private BigDecimal latitude;

    /**
     * Longitude GPS (-180 à +180)
     * Ex: 11.5167 pour Yaoundé
     * 
     * ⚠️ Validation constraint DB
     */
    @Column("longitude")
    private BigDecimal longitude;

    /**
     * ⚠️ GÉOMÉTRIE POSTGIS - NE PAS SETTER MANUELLEMENT
     * 
     * Généré automatiquement par trigger PostgreSQL:
     * geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
     * 
     * Utilisé pour recherche spatiale rapide via index GIST
     * 
     * 🔥 Ce champ rend recherches proximité 50x plus rapides !
     */
    // Note: En R2DBC, géométrie pas supportée nativement
    // On utilise lat/lon + fonctions PostGIS dans queries

    // ============================================================
    // ADRESSE STRUCTURÉE
    // ============================================================

    /**
     * Rue/Numéro
     * Ex: "Avenue de la Réunification"
     */
    @Column("address_street")
    private String addressStreet;

    /**
     * Ville (OBLIGATOIRE)
     * Ex: "Yaoundé", "Douala"
     * 
     * 🔍 Indexé pour filtrage rapide
     */
    @Column("address_city")
    private String addressCity;

    /**
     * Code postal
     * Ex: "BP 1234" (format Cameroun)
     */
    @Column("address_postal_code")
    private String addressPostalCode;

    /**
     * Région/Province
     * Ex: "Centre", "Littoral"
     */
    @Column("address_region")
    private String addressRegion;

    /**
     * Quartier/Voisinage
     */
    @Column("address_neighborhood")
    private String addressNeighborhood;

    /**
     * Pays (défaut Cameroun)
     */
    @Column("address_country")
    @Builder.Default
    private String addressCountry = "Cameroun";

    // ============================================================
    // CONTACT
    // ============================================================

    /**
     * Téléphone avec indicatif
     * Ex: "+237222234656"
     */
    @Column("phone")
    private String phone;

    /**
     * Email contact POI
     * Ex: "contact@hotel.cm"
     */
    @Column("email")
    private String email;

    /**
     * Site web
     * Ex: "https://hotel-hilton-yaounde.com"
     */
    @Column("website")
    private String website;

    // ============================================================
    // HORAIRES & ATTRIBUTS (JSONB pour flexibilité)
    // ============================================================

    /**
     * Horaires d'ouverture (JSONB)
     * Stocké en DB comme JSON string
     *
     * Format: {"monday": "09:00-18:00", "tuesday": "09:00-18:00", ...}
     *
     * 🔥 Avantage JSONB: requêtes directes possible
     * Ex: WHERE opening_hours->>'monday' != 'Closed'
     */
    @Column("opening_hours")
    private Json openingHoursJson;

    /**
     * Note moyenne (0.0 à 5.0)
     * Calculé automatiquement via trigger depuis reviews
     *
     * ⚠️ Ne PAS modifier manuellement
     */
    @Column("rating")
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    /**
     * Nombre total d'avis
     * Auto-incrémenté via trigger
     */
    @Column("review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    /**
     * Niveau de prix (1-4)
     * 1 = €, 2 = €€, 3 = €€€, 4 = €€€€
     */
    @Column("price_level")
    private Integer priceLevel;

    /**
     * Fourchette prix (format texte)
     * Ex: "€€", "€€€"
     */
    @Column("price_range")
    private String priceRange;

    // ============================================================
    // SERVICES & ÉQUIPEMENTS (JSONB arrays)
    // ============================================================

    /**
     * Services disponibles (JSONB array)
     * Ex: ["wifi", "parking", "restaurant", "piscine"]
     *
     * Stocké en DB comme JSON string
     */
    @Column("services")
    private Json servicesJson;

    /**
     * Équipements/Commodités (JSONB array)
     * Ex: ["wheelchair_accessible", "outdoor_seating"]
     *
     * Stocké en DB comme JSON string
     */
    @Column("amenities")
    private Json amenitiesJson;

    /**
     * Tags/étiquettes (JSONB array)
     * Ex: ["romantique", "vue", "terrasse"]
     *
     * Utilisé pour recommandations
     */
    @Column("tags")
    private Json tagsJson;

    // ============================================================
    // MÉTADONNÉES MÉDIA
    // ============================================================

    /**
     * URL image principale
     * Stocké sur Cloudinary/S3
     */
    @Column("image_url")
    private String imageUrl;

    /**
     * Images supplémentaires (JSONB array URLs)
     * Ex: ["https://...", "https://..."]
     *
     * Stocké en DB comme JSON string
     */
    @Column("images")
    private Json imagesJson;

    /**
     * Métadonnées supplémentaires (JSONB object)
     * Format libre selon type POI
     *
     * Ex Hotel: {"stars": 5, "rooms": 120}
     * Ex Restaurant: {"cuisine": "Italian", "chef": "Mario"}
     */
    @Column("metadata")
    private Json metadataJson;

    // ============================================================
    // STATUT & MODÉRATION
    // ============================================================

    /**
     * POI vérifié par administrateur
     * true = données confirmées fiables
     */
    @Column("is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * POI actif/visible
     * false = masqué (soft delete)
     */
    @Column("is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Nombre de vues
     * Incrémenté à chaque consultation
     */
    @Column("view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * Nombre d'ajouts en favoris
     * Auto-calculé via COUNT()
     */
    @Column("favorite_count")
    @Builder.Default
    private Integer favoriteCount = 0;

    @Column("like_count")
    @Builder.Default
    private Integer likeCount = 0;

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

    /**
     * Date vérification par admin
     * null tant que non vérifié
     */
    @Column("verified_at")
    private LocalDateTime verifiedAt;

    // ============================================================
    // CHAMPS TRANSIENTS (désérialisés depuis JSONB)
    // ============================================================

    /**
     * Catégorie complète (jointure)
     * Chargé à la demande pour éviter N+1
     */
    @Transient
    private PoiCategory category;

    /**
     * Horaires d'ouverture (Map Java)
     * Désérialisé depuis openingHoursJson
     */
    @Transient
    private Map<String, String> openingHours;

    /**
     * Services (List Java)
     * Désérialisé depuis servicesJson
     */
    @Transient
    private List<String> services;

    /**
     * Amenities (List Java)
     * Désérialisé depuis amenitiesJson
     */
    @Transient
    private List<String> amenities;

    /**
     * Tags (List Java)
     * Désérialisé depuis tagsJson
     */
    @Transient
    private List<String> tags;

    /**
     * Images supplémentaires (List Java)
     * Désérialisé depuis imagesJson
     */
    @Transient
    private List<String> images;

    /**
     * Métadonnées (Map Java)
     * Désérialisé depuis metadataJson
     */
    @Transient
    private Map<String, Object> metadata;

    // ============================================================
    // MÉTHODES UTILITAIRES (Sérialisation JSONB)
    // ============================================================

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Convertit openingHours Map → JSON string pour DB
     * Appelé avant save()
     */
    public void serializeOpeningHours() {
        if (this.openingHours != null) {
            try {
                this.openingHoursJson = Json.of(JSON_MAPPER.writeValueAsString(this.openingHours));
            } catch (JsonProcessingException e) {
                log.error("Erreur sérialisation openingHours", e);
                this.openingHoursJson = Json.of("{}");
            }
        }
    }

    /**
     * Convertit JSON string → openingHours Map
     * Appelé après chargement DB
     */
    public void deserializeOpeningHours() {
        if (this.openingHoursJson != null) {
            try {
                this.openingHours = JSON_MAPPER.readValue(
                        this.openingHoursJson.asString(),
                        new TypeReference<Map<String, String>>() {
                        });
            } catch (JsonProcessingException e) {
                log.error("Erreur désérialisation openingHours", e);
                this.openingHours = new HashMap<>();
            }
        }
    }

    /**
     * Convertit services List → JSON string
     */
    public void serializeServices() {
        if (this.services != null) {
            try {
                this.servicesJson = Json.of(JSON_MAPPER.writeValueAsString(this.services));
            } catch (JsonProcessingException e) {
                log.error("Erreur sérialisation services", e);
                this.servicesJson = Json.of("[]");
            }
        }
    }

    /**
     * Convertit JSON string → services List
     */
    public void deserializeServices() {
        if (this.servicesJson != null) {
            try {
                this.services = JSON_MAPPER.readValue(
                        this.servicesJson.asString(),
                        new TypeReference<List<String>>() {
                        });
            } catch (JsonProcessingException e) {
                log.error("Erreur désérialisation services", e);
                this.services = new ArrayList<>();
            }
        }
    }

    /**
     * Convertit amenities List → JSON string
     */
    public void serializeAmenities() {
        if (this.amenities != null) {
            try {
                this.amenitiesJson = Json.of(JSON_MAPPER.writeValueAsString(this.amenities));
            } catch (JsonProcessingException e) {
                log.error("Erreur sérialisation amenities", e);
                this.amenitiesJson = Json.of("[]");
            }
        }
    }

    /**
     * Convertit JSON string → amenities List
     */
    public void deserializeAmenities() {
        if (this.amenitiesJson != null) {
            try {
                this.amenities = JSON_MAPPER.readValue(
                        this.amenitiesJson.asString(),
                        new TypeReference<List<String>>() {
                        });
            } catch (JsonProcessingException e) {
                log.error("Erreur désérialisation amenities", e);
                this.amenities = new ArrayList<>();
            }
        }
    }

    /**
     * Convertit tags List → JSON string
     */
    public void serializeTags() {
        if (this.tags != null) {
            try {
                this.tagsJson = Json.of(JSON_MAPPER.writeValueAsString(this.tags));
            } catch (JsonProcessingException e) {
                log.error("Erreur sérialisation tags", e);
                this.tagsJson = Json.of("[]");
            }
        }
    }

    /**
     * Convertit JSON string → tags List
     */
    public void deserializeTags() {
        if (this.tagsJson != null) {
            try {
                this.tags = JSON_MAPPER.readValue(
                        this.tagsJson.asString(),
                        new TypeReference<List<String>>() {
                        });
            } catch (JsonProcessingException e) {
                log.error("Erreur désérialisation tags", e);
                this.tags = new ArrayList<>();
            }
        }
    }

    /**
     * Convertit images List → JSON string
     */
    public void serializeImages() {
        if (this.images != null) {
            try {
                this.imagesJson = Json.of(JSON_MAPPER.writeValueAsString(this.images));
            } catch (JsonProcessingException e) {
                log.error("Erreur sérialisation images", e);
                this.imagesJson = Json.of("[]");
            }
        }
    }

    /**
     * Convertit JSON string → images List
     */
    public void deserializeImages() {
        if (this.imagesJson != null) {
            try {
                this.images = JSON_MAPPER.readValue(
                        this.imagesJson.asString(),
                        new TypeReference<List<String>>() {
                        });
            } catch (JsonProcessingException e) {
                log.error("Erreur désérialisation images", e);
                this.images = new ArrayList<>();
            }
        }
    }

    /**
     * Convertit metadata Map → JSON string
     */
    public void serializeMetadata() {
        if (this.metadata != null) {
            try {
                this.metadataJson = Json.of(JSON_MAPPER.writeValueAsString(this.metadata));
            } catch (JsonProcessingException e) {
                log.error("Erreur sérialisation metadata", e);
                this.metadataJson = Json.of("{}");
            }
        }
    }

    /**
     * Convertit JSON string → metadata Map
     */
    public void deserializeMetadata() {
        if (this.metadataJson != null) {
            try {
                this.metadata = JSON_MAPPER.readValue(
                        this.metadataJson.asString(),
                        new TypeReference<Map<String, Object>>() {
                        });
            } catch (JsonProcessingException e) {
                log.error("Erreur désérialisation metadata", e);
                this.metadata = new HashMap<>();
            }
        }
    }

    /**
     * Sérialise TOUS les champs JSONB avant save
     * À appeler dans Service avant repository.save()
     */
    public void serializeAllJsonFields() {
        serializeOpeningHours();
        serializeServices();
        serializeAmenities();
        serializeTags();
        serializeImages();
        serializeMetadata();
    }

    /**
     * Désérialise TOUS les champs JSONB après load
     * À appeler dans Service après repository.find()
     */
    public void deserializeAllJsonFields() {
        deserializeOpeningHours();
        deserializeServices();
        deserializeAmenities();
        deserializeTags();
        deserializeImages();
        deserializeMetadata();
    }

    // ============================================================
    // MÉTHODES MÉTIER
    // ============================================================

    /**
     * Incrémente le compteur de vues
     */
    public void incrementViewCount() {
        this.viewCount = (this.viewCount != null ? this.viewCount : 0) + 1;
    }

    /**
     * Marque comme vérifié par admin
     */
    public void verify() {
        this.isVerified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * Désactive le POI (soft delete)
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Réactive le POI
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Calcule score popularité
     * Formule: (favoris × 3) + (reviews × 2) + vues
     * 
     * @return Score popularité
     */
    public Long calculatePopularityScore() {
        long favorites = this.favoriteCount != null ? this.favoriteCount : 0;
        long reviews = this.reviewCount != null ? this.reviewCount : 0;
        long views = this.viewCount != null ? this.viewCount : 0;

        return (favorites * 3) + (reviews * 2) + views;
    }

    /**
     * Retourne l'adresse complète formatée
     * 
     * @return Adresse complète
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();

        if (addressStreet != null && !addressStreet.isBlank()) {
            address.append(addressStreet).append(", ");
        }

        if (addressCity != null) {
            address.append(addressCity);
        }

        if (addressRegion != null && !addressRegion.isBlank()) {
            address.append(", ").append(addressRegion);
        }

        if (addressCountry != null && !addressCountry.equals("Cameroun")) {
            address.append(", ").append(addressCountry);
        }

        return address.toString();
    }
}