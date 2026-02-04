package com.enspy.tripplanning.poi.repository;

import com.enspy.tripplanning.poi.entity.Poi;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PoiRepository extends R2dbcRepository<Poi, Long> {

       Flux<Poi> findByCategoryId(Long categoryId, Pageable pageable);

       Mono<Long> countByCategoryId(Long categoryId);

       Flux<Poi> findAllBy(Pageable pageable);

       @Query("SELECT p.* FROM pois p JOIN poi_categories c ON p.category_id = c.category_id WHERE c.name = :categoryName")
       Flux<Poi> findByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

       @Query("SELECT COUNT(p.*) FROM pois p JOIN poi_categories c ON p.category_id = c.category_id WHERE c.name = :categoryName")
       Mono<Long> countByCategoryName(@Param("categoryName") String categoryName);

       @Query("SELECT * FROM pois WHERE name ILIKE CONCAT('%', :search, '%') OR description ILIKE CONCAT('%', :search, '%')")
       Flux<Poi> searchByNameOrDescription(@Param("search") String search, Pageable pageable);

       @Query("SELECT COUNT(*) FROM pois WHERE name ILIKE CONCAT('%', :search, '%') OR description ILIKE CONCAT('%', :search, '%')")
       Mono<Long> countBySearch(@Param("search") String search);

       // ================================================================
       // RECHERCHE PAR PROXIMITÉ - OPTIMISÉE AVEC POSTGIS
       // ================================================================

       /**
        * Recherche POIs par proximité avec PostGIS natif (ST_DWithin).
        * 
        * PERFORMANCE: Utilise l'index spatial GIST pour recherche ultra-rapide.
        * ST_DWithin est 10x plus rapide que le calcul Haversine manuel.
        * 
        * L'opérateur ::geography convertit en coordonnées sphériques pour
        * calcul de distance précis sur la Terre (vs planar).
        * 
        * @param latitude  Latitude du centre
        * @param longitude Longitude du centre
        * @param radius    Rayon en kilomètres
        * @param limit     Nombre max de résultats
        * @param offset    Offset pour pagination
        * @return Flux de POIs dans le rayon, triés par distance
        */
       @Query("""
                     SELECT p.*,
                            ST_Distance(p.geom::geography,
                                       ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) / 1000 AS distance_km
                     FROM pois p
                     WHERE ST_DWithin(p.geom::geography,
                                     ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                                     :radius * 1000)
                       AND p.is_active = TRUE
                     ORDER BY p.geom <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
                     LIMIT :limit OFFSET :offset
                     """)
       Flux<Poi> findByProximity(
                     @Param("lat") Double latitude,
                     @Param("lon") Double longitude,
                     @Param("radius") Double radius,
                     @Param("limit") Integer limit,
                     @Param("offset") Integer offset);

       /**
        * Compte POIs dans un rayon avec PostGIS.
        * 
        * @param latitude  Latitude du centre
        * @param longitude Longitude du centre
        * @param radius    Rayon en kilomètres
        * @return Nombre de POIs dans le rayon
        */
       @Query("""
                     SELECT COUNT(*)
                     FROM pois p
                     WHERE ST_DWithin(p.geom::geography,
                                     ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                                     :radius * 1000)
                       AND p.is_active = TRUE
                     """)
       Mono<Long> countByProximity(
                     @Param("lat") Double latitude,
                     @Param("lon") Double longitude,
                     @Param("radius") Double radius);

       // ================================================================
       // RECHERCHE DANS ZONE (BOUNDING BOX) - OPTIMISÉE AVEC POSTGIS
       // ================================================================

       /**
        * Recherche POIs dans une bounding box avec PostGIS.
        * 
        * PERFORMANCE: L'opérateur && utilise l'index GIST pour filtrage ultra-rapide.
        * ST_MakeEnvelope crée un rectangle géographique.
        * 
        * @param minLat Latitude minimale
        * @param minLon Longitude minimale
        * @param maxLat Latitude maximale
        * @param maxLon Longitude maximale
        * @return Flux de POIs dans la zone
        */
       @Query("""
                     SELECT p.* FROM pois p
                     WHERE p.geom && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
                       AND p.is_active = TRUE
                     """)
       Flux<Poi> findInBoundingBox(
                     @Param("minLat") Double minLat,
                     @Param("minLon") Double minLon,
                     @Param("maxLat") Double maxLat,
                     @Param("maxLon") Double maxLon);

       /**
        * Recherche POIs dans une bounding box avec filtres de catégories.
        * 
        * @param minLat      Latitude minimale
        * @param minLon      Longitude minimale
        * @param maxLat      Latitude maximale
        * @param maxLon      Longitude maximale
        * @param categoryIds IDs des catégories à filtrer
        * @return Flux de POIs dans la zone et catégories
        */
       @Query("""
                     SELECT p.* FROM pois p
                     WHERE p.geom && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
                       AND (:categoryIds IS NULL OR p.category_id = ANY(:categoryIds))
                       AND p.is_active = TRUE
                     """)
       Flux<Poi> findInBoundingBoxByCategories(
                     @Param("minLat") Double minLat,
                     @Param("minLon") Double minLon,
                     @Param("maxLat") Double maxLat,
                     @Param("maxLon") Double maxLon,
                     @Param("categoryIds") Long[] categoryIds);

       // Top POI par rating pour dashboard
       @Query("SELECT * FROM pois ORDER BY rating DESC, review_count DESC LIMIT 5")
       Flux<Poi> findTop5ByOrderByRatingDescReviewCountDesc();

       // Count POI par créateur (pour statistiques utilisateur)
       @Query("SELECT COUNT(*) FROM pois WHERE created_by_user_id = :userId")
       Mono<Long> countByCreatedByUserId(@Param("userId") java.util.UUID userId);

       @Query("""
                     SELECT p.* FROM pois p
                     JOIN poi_favorites f ON p.poi_id = f.poi_id
                     WHERE f.user_id = :userId
                     ORDER BY p.like_count DESC, p.rating DESC
                     LIMIT 5
                     """)
       Flux<Poi> findTopFavoritedByUserIdOrderByLikes(@Param("userId") java.util.UUID userId);
}
