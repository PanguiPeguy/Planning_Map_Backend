package com.enspy.tripplanning.poi.repository;

import com.enspy.tripplanning.poi.entity.PoiLike;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface PoiLikeRepository extends R2dbcRepository<PoiLike, Long> {
    Mono<PoiLike> findByPoiIdAndUserId(Long poiId, UUID userId);

    Mono<Void> deleteByPoiIdAndUserId(Long poiId, UUID userId);

    Mono<Long> countByPoiId(Long poiId);

    Mono<Boolean> existsByPoiIdAndUserId(Long poiId, UUID userId);

    Mono<Long> countByUserId(UUID userId);
}
