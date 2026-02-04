package com.enspy.tripplanning.planning.controller;

import com.enspy.tripplanning.authentification.entity.User;
import com.enspy.tripplanning.planning.dto.ExternalPlanningDTO;
import com.enspy.tripplanning.planning.dto.ImportPlanningRequest;
import com.enspy.tripplanning.planning.entity.Planning;
import com.enspy.tripplanning.planning.entity.PlanningItem;
import com.enspy.tripplanning.planning.service.ExternalPlanningService;
import com.enspy.tripplanning.planning.service.PlanningService;
import com.enspy.tripplanning.planning.service.RouteOptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/planning")
@RequiredArgsConstructor
@Tag(name = "Planning", description = "Gestion des plannings et calculs d'itinéraires")
public class PlanningController {

    private final PlanningService planningService;
    private final ExternalPlanningService externalPlanningService;
    private final RouteOptimizationService routeOptimizationService;

    @Operation(summary = "Lister les plannings externes disponibles", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/external")
    public Flux<ExternalPlanningDTO> getExternalPlannings(@AuthenticationPrincipal User user) {
        return externalPlanningService.getAvailablePlannings(user.getUserId());
    }

    @Operation(summary = "Importer un planning externe", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Planning> importPlanning(@AuthenticationPrincipal User user,
            @RequestBody ImportPlanningRequest request) {
        log.info("Importation du planning: {} pour l'utilisateur: {}", request.getName(), user.getEmail());
        return planningService.importPlanning(user.getUserId(), request.getExternalId(), request.getName());
    }

    @Operation(summary = "Lister les plannings locaux de l'utilisateur", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/local")
    public Flux<Planning> getLocalPlannings(@AuthenticationPrincipal User user) {
        return planningService.getUserPlannings(user.getUserId());
    }

    @Operation(summary = "Récupérer les détails d'un planning", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/local/{id}")
    public Mono<Planning> getPlanning(@PathVariable UUID id) {
        return planningService.getPlanningById(id);
    }

    @Operation(summary = "Récupérer les items d'un planning", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/local/{id}/items")
    public Flux<PlanningItem> getPlanningItems(@PathVariable UUID id) {
        return planningService.getPlanningItems(id);
    }

    @Operation(summary = "Calculer l'itinéraire pour un item", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/items/{itemId}/calculate")
    public Mono<PlanningItem> calculateItemRoute(@PathVariable UUID itemId) {
        return routeOptimizationService.calculateRouteForItem(itemId);
    }

    @Operation(summary = "Calculer tous les itinéraires d'un planning", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/local/{id}/calculate-all")
    public Flux<PlanningItem> calculateAllRoutes(@PathVariable UUID id) {
        return routeOptimizationService.calculateAllRoutesForPlanning(id);
    }

    @Operation(summary = "Assigner un itinéraire sauvegardé à une étape", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/items/{itemId}/assign-itinerary/{itineraryId}")
    public Mono<PlanningItem> assignItinerary(@PathVariable UUID itemId, @PathVariable UUID itineraryId) {
        return routeOptimizationService.assignItineraryToItem(itemId, itineraryId);
    }

    @Operation(summary = "Ajouter un item à un planning", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/local/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PlanningItem> addItem(@PathVariable UUID id, @RequestBody PlanningItem item) {
        return planningService.addPlanningItem(id, item);
    }

    @Operation(summary = "Mettre à jour un item de planning", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/items/{itemId}")
    public Mono<PlanningItem> updateItem(@PathVariable UUID itemId, @RequestBody PlanningItem item) {
        return planningService.updatePlanningItem(itemId, item);
    }

    @Operation(summary = "Supprimer un item de planning", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteItem(@PathVariable UUID itemId) {
        return planningService.deletePlanningItem(itemId);
    }

    @Operation(summary = "Récupérer un item par son ID", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/items/{itemId}")
    public Mono<PlanningItem> getItem(@PathVariable UUID itemId) {
        return planningService.getPlanningItemById(itemId);
    }
}
