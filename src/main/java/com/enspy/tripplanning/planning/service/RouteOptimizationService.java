package com.enspy.tripplanning.planning.service;

import com.enspy.tripplanning.itinerary.repository.ItineraryRepository;
import com.enspy.tripplanning.planning.entity.PlanningItem;
import com.enspy.tripplanning.planning.entity.Planning;
import com.enspy.tripplanning.planning.repository.PlanningItemRepository;
import com.enspy.tripplanning.planning.repository.PlanningRepository;
import com.enspy.tripplanning.routing.dto.RouteRequest;
import com.enspy.tripplanning.routing.service.RouteCalculatorService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteOptimizationService {

    private final RouteCalculatorService routeCalculatorService;
    private final GeocodingService geocodingService;
    private final PlanningItemRepository planningItemRepository;
    private final PlanningRepository planningRepository;
    private final ItineraryRepository itineraryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<PlanningItem> calculateRouteForItem(UUID itemId) {
        return planningItemRepository.findById(itemId)
                .flatMap(this::optimizeItem);
    }

    public Flux<PlanningItem> calculateAllRoutesForPlanning(UUID planningId) {
        return planningItemRepository.findAllByPlanningId(planningId)
                .flatMap(this::optimizeItem, 5);
    }

    // New method to assign an itinerary
    public Mono<PlanningItem> assignItineraryToItem(UUID itemId, UUID itineraryId) {
        return planningItemRepository.findById(itemId)
                .flatMap(item -> {
                    item.setItineraryId(itineraryId);
                    // Reset status to force re-calculation/optimization context if needed
                    // Or immediately fetch itinerary and update
                    return optimizeItem(item);
                });
    }

    private Mono<PlanningItem> optimizeItem(PlanningItem item) {
        Double startLat = item.getOriginLatitude();
        Double startLon = item.getOriginLongitude();
        Double endLat = item.getDestinationLatitude();
        Double endLon = item.getDestinationLongitude();

        // Si les coordonnées précises manquent, on géocode les villes
        if (startLat == null || startLon == null) {
            Point p = geocodingService.getCoordinates(item.getOriginCity()).orElse(null);
            if (p != null) {
                startLat = p.getY();
                startLon = p.getX();
            }
        }

        if (endLat == null || endLon == null) {
            Point p = geocodingService.getCoordinates(item.getDestinationCity()).orElse(null);
            if (p != null) {
                endLat = p.getY();
                endLon = p.getX();
            }
        }

        if (startLat == null || endLat == null) {
            log.warn("Coordinates not found for {} or {}", item.getOriginCity(), item.getDestinationCity());
            return Mono.just(item);
        }

        List<Long> waypointIds = new ArrayList<>();
        if (item.getSelectedWaypointsJson() != null && !item.getSelectedWaypointsJson().isEmpty()) {
            try {
                waypointIds = objectMapper.readValue(item.getSelectedWaypointsJson(), new TypeReference<List<Long>>() {
                });
            } catch (Exception e) {
                log.error("Error parsing waypoints JSON for item {}: {}", item.getId(), e.getMessage());
            }
        }

        final Double finalStartLat = startLat;
        final Double finalStartLon = startLon;
        final Double finalEndLat = endLat;
        final Double finalEndLon = endLon;

        // 1. Calculate Optimal Route (Always done for comparison)
        RouteRequest request = RouteRequest.builder()
                .startLatitude(finalStartLat)
                .startLongitude(finalStartLon)
                .endLatitude(finalEndLat)
                .endLongitude(finalEndLon)
                .waypointPoiIds(waypointIds)
                .build();

        log.info("🔵 Calling RouteCalculatorService.calculateRoute() for item {} with waypoints: {}",
                item.getId(), waypointIds);
        log.debug("Route request: start=({}, {}), end=({}, {})",
                finalStartLat, finalStartLon, finalEndLat, finalEndLon);

        return routeCalculatorService.calculateRoute(request)
                .doOnNext(response -> {
                    log.info("🟢 RouteCalculatorService returned: found={}, hasCustom={}, hasOptimal={}",
                            response.getFound(),
                            response.getCustomRoute() != null && response.getCustomRoute().getFound(),
                            response.getOptimalRoute() != null && response.getOptimalRoute().getFound());
                    if (response.getCustomRoute() != null) {
                        log.debug("Custom route geometry length: {}",
                                response.getCustomRoute().getGeometryEncoded() != null
                                        ? response.getCustomRoute().getGeometryEncoded().length()
                                        : 0);
                    }
                    if (response.getOptimalRoute() != null) {
                        log.debug("Optimal route geometry length: {}",
                                response.getOptimalRoute().getGeometryEncoded() != null
                                        ? response.getOptimalRoute().getGeometryEncoded().length()
                                        : 0);
                    }
                })
                .flatMap(response -> {
                    // Sauvegarder les coordonnées s'il s'agissait de géocodage
                    item.setOriginLatitude(finalStartLat);
                    item.setOriginLongitude(finalStartLon);
                    item.setDestinationLatitude(finalEndLat);
                    item.setDestinationLongitude(finalEndLon);

                    // Check if routes are actually valid (have geometry)
                    boolean hasOptimalRoute = response != null &&
                            response.getOptimalRoute() != null &&
                            response.getOptimalRoute().getFound() != null &&
                            response.getOptimalRoute().getFound() &&
                            response.getOptimalRoute().getGeometryEncoded() != null &&
                            !response.getOptimalRoute().getGeometryEncoded().trim().isEmpty();

                    boolean hasCustomRoute = response != null &&
                            response.getCustomRoute() != null &&
                            response.getCustomRoute().getFound() != null &&
                            response.getCustomRoute().getFound() &&
                            response.getCustomRoute().getGeometryEncoded() != null &&
                            !response.getCustomRoute().getGeometryEncoded().trim().isEmpty();

                    // Update optimal route if available
                    if (hasOptimalRoute) {
                        var optimal = response.getOptimalRoute();
                        item.setOptimalRouteGeom(optimal.getGeometryEncoded());
                        log.debug("Optimal route saved for item {} ({} to {}) - Distance: {} km, Time: {} s",
                                item.getId(), item.getOriginCity(), item.getDestinationCity(),
                                optimal.getTotalDistanceKm(), optimal.getTotalTimeSeconds());
                    } else {
                        // Clear optimal route if invalid
                        item.setOptimalRouteGeom(null);
                    }

                    // Update custom route if available and no itinerary is assigned
                    if (item.getItineraryId() == null && hasCustomRoute) {
                        var custom = response.getCustomRoute();
                        item.setRouteGeom(custom.getGeometryEncoded());
                        if (custom.getTotalDistanceKm() != null) {
                            item.setDistanceMeters(custom.getTotalDistanceKm() * 1000);
                        }
                        if (custom.getTotalTimeSeconds() != null) {
                            item.setTravelTimeSeconds(custom.getTotalTimeSeconds());
                        }
                        log.debug("Custom route saved for item {} ({} to {})", item.getId(),
                                item.getOriginCity(), item.getDestinationCity());
                    } else if (item.getItineraryId() == null && !hasCustomRoute) {
                        // Clear custom route if invalid and no itinerary assigned
                        item.setRouteGeom(null);
                        item.setDistanceMeters(null);
                        item.setTravelTimeSeconds(null);
                    }

                    // Update status based on whether we found at least one valid route
                    if (hasOptimalRoute || hasCustomRoute) {
                        item.setStatus("CALCULATED");
                        log.info("Route calculation successful for item {} - Custom: {}, Optimal: {}",
                                item.getId(), hasCustomRoute, hasOptimalRoute);
                    } else {
                        item.setStatus("FAILED");
                        log.warn("No valid route found for item {} ({} to {}) - geometry is null or empty",
                                item.getId(), item.getOriginCity(), item.getDestinationCity());
                    }

                    // 2. Si un itinéraire enregistré est sélectionné, il surcharge la route
                    // personnalisée
                    if (item.getItineraryId() != null) {
                        return itineraryRepository.findById(item.getItineraryId())
                                .map(itinerary -> {
                                    if (itinerary.getGeometryEncoded() != null
                                            && !itinerary.getGeometryEncoded().trim().isEmpty()) {
                                        item.setRouteGeom(itinerary.getGeometryEncoded());
                                        item.setDistanceMeters(itinerary.getDistanceMeters());
                                        item.setTravelTimeSeconds(itinerary.getDurationSeconds());
                                        item.setStatus("CALCULATED");
                                        log.debug("Route for item {} overridden by itinerary {}", item.getId(),
                                                itinerary.getId());
                                    } else {
                                        log.warn("Itinerary {} assigned to item {} has no geometry!", itinerary.getId(),
                                                item.getId());
                                    }
                                    return item;
                                })
                                .defaultIfEmpty(item);
                    }

                    return Mono.just(item);
                })
                .flatMap(updatedItem -> {
                    // Final status check: if we have any geometry, it's CALCULATED
                    if ((updatedItem.getRouteGeom() != null && !updatedItem.getRouteGeom().isEmpty()) ||
                            (updatedItem.getOptimalRouteGeom() != null
                                    && !updatedItem.getOptimalRouteGeom().isEmpty())) {
                        updatedItem.setStatus("CALCULATED");
                    } else if (!"CALCULATED".equals(updatedItem.getStatus())) {
                        updatedItem.setStatus("FAILED");
                    }
                    return planningItemRepository.save(updatedItem)
                            .flatMap(savedItem -> checkPlanningCompletion(savedItem.getPlanningId())
                                    .thenReturn(savedItem));
                });
    }

    private Mono<Void> checkPlanningCompletion(UUID planningId) {
        return planningItemRepository.findAllByPlanningId(planningId)
                .collectList()
                .flatMap(items -> {
                    boolean allDone = items.stream().allMatch(
                            item -> "CALCULATED".equals(item.getStatus()) || "FAILED".equals(item.getStatus()));

                    if (allDone) {
                        return planningRepository.findById(planningId)
                                .flatMap(planning -> {
                                    planning.setStatus("COMPLETED");
                                    return planningRepository.save(planning);
                                }).then();
                    }
                    return Mono.empty();
                });
    }
}
