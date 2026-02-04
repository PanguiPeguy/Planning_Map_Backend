package com.enspy.tripplanning.planning.repository;

import com.enspy.tripplanning.planning.entity.Planning;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface PlanningRepository extends ReactiveCrudRepository<Planning, UUID> {
    Flux<Planning> findAllByUserId(UUID userId);
}
