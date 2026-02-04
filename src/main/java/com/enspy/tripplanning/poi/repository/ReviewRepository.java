package com.enspy.tripplanning.poi.repository;

import com.enspy.tripplanning.poi.entity.Review;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface ReviewRepository extends R2dbcRepository<Review, Long> {

    Flux<Review> findByPoiId(Long poiId);

    Flux<Review> findByPoiIdOrderByCreatedAtDesc(Long poiId);

    Flux<Review> findByUserId(UUID userId);

    Mono<Boolean> existsByPoiIdAndUserId(Long poiId, UUID userId);

    Mono<Long> countByPoiId(Long poiId);
}