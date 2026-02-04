package com.enspy.tripplanning.planning.service;

import com.enspy.tripplanning.planning.dto.ExternalPlanningDTO;
import com.enspy.tripplanning.planning.entity.PlanningItem;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
public class ExternalPlanningService {

    /**
     * Simulation de la récupération des plannings disponibles chez l'autre groupe.
     * En conditions réelles, cela ferait un appel WebClient vers leur API.
     */
    public Flux<ExternalPlanningDTO> getAvailablePlannings(UUID userId) {
        return Flux.just(
                ExternalPlanningDTO.builder()
                        .externalId(UUID.randomUUID())
                        .name("Planning Tournée Nord v1")
                        .description("Itinéraire prévisionnel pour la zone septentrionale")
                        .build(),
                ExternalPlanningDTO.builder()
                        .externalId(UUID.randomUUID())
                        .name("Planning Livraison Douala-Ouest")
                        .description("Planning hebdomadaire des livraisons urbaines")
                        .build(),
                ExternalPlanningDTO.builder()
                        .externalId(UUID.randomUUID())
                        .name("Planning Mission Yaoundé")
                        .description("Déplacements administratifs Janvier 2026")
                        .build());
    }

    /**
     * Simulation de la récupération du contenu détaillé d'un planning externe.
     */
    public Flux<PlanningItem> getExternalPlanningItems(UUID externalId) {
        // Mocking some items for any external ID provided
        return Flux.just(
                PlanningItem.builder()
                        .originCity("Douala")
                        .destinationCity("Yaoundé")
                        .plannedDate(LocalDate.now().plusDays(1))
                        .departureTime(LocalTime.of(8, 0))
                        .build(),
                PlanningItem.builder()
                        .originCity("Yaoundé")
                        .destinationCity("Bafoussam")
                        .plannedDate(LocalDate.now().plusDays(2))
                        .departureTime(LocalTime.of(9, 30))
                        .build(),
                PlanningItem.builder()
                        .originCity("Bafoussam")
                        .destinationCity("Bamenda")
                        .plannedDate(LocalDate.now().plusDays(3))
                        .departureTime(LocalTime.of(14, 0))
                        .build());
    }
}
