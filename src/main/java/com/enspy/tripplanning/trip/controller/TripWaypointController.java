package com.enspy.tripplanning.trip.controller;

import com.enspy.tripplanning.trip.dto.AddWaypointRequest;
import com.enspy.tripplanning.trip.dto.TripWaypointDTO;
import com.enspy.tripplanning.trip.service.TripWaypointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/trips/{tripId}/waypoints")
@RequiredArgsConstructor
@Tag(name = "Trip Waypoints", description = "Gestion des étapes de voyage")
public class TripWaypointController {

    private final TripWaypointService waypointService;

    @Operation(summary = "Ajouter une étape", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TripWaypointDTO> addWaypoint(
        @PathVariable UUID tripId,
        @Valid @RequestBody AddWaypointRequest request,
        Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        return waypointService.addWaypoint(tripId, request, userId);
    }

    @Operation(summary = "Lister les étapes", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public Flux<TripWaypointDTO> getWaypoints(@PathVariable UUID tripId) {
        return waypointService.getTripWaypoints(tripId);
    }

    @Operation(summary = "Modifier une étape", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{waypointId}")
    public Mono<TripWaypointDTO> updateWaypoint(
        @PathVariable UUID tripId,
        @PathVariable Long waypointId,
        @Valid @RequestBody AddWaypointRequest request
    ) {
        return waypointService.updateWaypoint(tripId, waypointId, request);
    }

    @Operation(summary = "Supprimer une étape", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{waypointId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteWaypoint(
        @PathVariable UUID tripId,
        @PathVariable Long waypointId
    ) {
        return waypointService.deleteWaypoint(tripId, waypointId);
    }

    private UUID extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Non authentifié");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof com.enspy.tripplanning.authentification.entity.User) {
            return ((com.enspy.tripplanning.authentification.entity.User) principal).getUserId();
        }
        
        throw new RuntimeException("Type principal invalide");
    }
}