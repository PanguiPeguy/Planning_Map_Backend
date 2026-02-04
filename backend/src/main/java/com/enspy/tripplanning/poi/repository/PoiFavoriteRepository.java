package com.enspy.tripplanning.poi.repository;

import com.enspy.tripplanning.poi.entity.PoiFavorite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.Map;

@Repository
public interface PoiFavoriteRepository extends R2dbcRepository<PoiFavorite, Long> {
    Mono<PoiFavorite> findByPoiIdAndUserId(Long poiId, UUID userId);

    Mono<Void> deleteByPoiIdAndUserId(Long poiId, UUID userId);

    Flux<PoiFavorite> findByUserId(UUID userId);

    Mono<Long> countByPoiId(Long poiId);

    Mono<Boolean> existsByPoiIdAndUserId(Long poiId, UUID userId);

    @Query("SELECT p.* FROM pois p JOIN poi_favorites f ON p.poi_id = f.poi_id WHERE f.user_id = :userId")
    Flux<com.enspy.tripplanning.poi.entity.Poi> findFavoritePoisByUserId(UUID userId, Pageable pageable);

    Mono<Long> countByUserId(UUID userId);

    Flux<PoiFavorite> findByPoiId(Long poiId);

    @Query("SELECT c.name as category, COUNT(f.favorite_id) as count FROM poi_favorites f JOIN pois p ON f.poi_id = p.poi_id JOIN poi_categories c ON p.category_id = c.category_id WHERE f.user_id = :userId GROUP BY c.name")
    Flux<CategoryCountProjection> findCategoryDistributionByUserId(UUID userId);
}
