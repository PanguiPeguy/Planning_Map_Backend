package com.enspy.tripplanning.planning.repository;

import com.enspy.tripplanning.planning.entity.PlanningItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface PlanningItemRepository extends ReactiveCrudRepository<PlanningItem, UUID> {
    Flux<PlanningItem> findAllByPlanningId(UUID planningId);
}
