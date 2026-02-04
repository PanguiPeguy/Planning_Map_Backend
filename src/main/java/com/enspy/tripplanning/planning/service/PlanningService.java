package com.enspy.tripplanning.planning.service;

import com.enspy.tripplanning.planning.entity.Planning;
import com.enspy.tripplanning.planning.entity.PlanningItem;
import com.enspy.tripplanning.planning.repository.PlanningItemRepository;
import com.enspy.tripplanning.planning.repository.PlanningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanningService {

    private final PlanningRepository planningRepository;
    private final PlanningItemRepository planningItemRepository;
    private final ExternalPlanningService externalPlanningService;

    public Flux<Planning> getUserPlannings(UUID userId) {
        return planningRepository.findAllByUserId(userId);
    }

    public Mono<Planning> getPlanningById(UUID id) {
        return planningRepository.findById(id);
    }

    public Flux<PlanningItem> getPlanningItems(UUID planningId) {
        return planningItemRepository.findAllByPlanningId(planningId);
    }

    public Mono<Planning> importPlanning(UUID userId, UUID externalId, String name) {
        Planning newPlanning = Planning.builder()
                .id(UUID.randomUUID())
                .name(name)
                .userId(userId)
                .status("DRAFT")
                .build();
        newPlanning.setNew(true);

        return planningRepository.save(newPlanning)
                .flatMap(savedPlanning -> externalPlanningService.getExternalPlanningItems(externalId)
                        .map(item -> {
                            item.setId(UUID.randomUUID());
                            item.setPlanningId(savedPlanning.getId());
                            item.setNew(true);
                            return item;
                        })
                        .collectList()
                        .flatMap(items -> planningItemRepository.saveAll(items).collectList())
                        .thenReturn(savedPlanning));
    }

    public Mono<Void> deletePlanning(UUID id) {
        return planningRepository.deleteById(id);
    }

    public Mono<PlanningItem> addPlanningItem(UUID planningId, PlanningItem item) {
        item.setId(UUID.randomUUID());
        item.setPlanningId(planningId);
        item.setStatus("DRAFT");
        item.setNew(true);
        return planningItemRepository.save(item);
    }

    public Mono<PlanningItem> updatePlanningItem(UUID itemId, PlanningItem item) {
        return planningItemRepository.findById(itemId)
                .flatMap(existing -> {
                    item.setId(itemId);
                    item.setPlanningId(existing.getPlanningId());
                    // On ne met pas setNew(true) car c'est un update
                    return planningItemRepository.save(item);
                });
    }

    public Mono<Void> deletePlanningItem(UUID itemId) {
        return planningItemRepository.deleteById(itemId);
    }

    public Mono<PlanningItem> getPlanningItemById(UUID itemId) {
        return planningItemRepository.findById(itemId);
    }
}
