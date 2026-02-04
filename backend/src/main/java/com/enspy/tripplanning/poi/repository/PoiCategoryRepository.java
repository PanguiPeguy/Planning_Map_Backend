package com.enspy.tripplanning.poi.repository;

import com.enspy.tripplanning.poi.entity.PoiCategory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PoiCategoryRepository extends R2dbcRepository<PoiCategory, Long> {

    Mono<PoiCategory> findByName(String name);

    Flux<PoiCategory> findAllByOrderByNameAsc();
}