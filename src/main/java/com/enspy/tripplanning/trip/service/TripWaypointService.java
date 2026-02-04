package com.enspy.tripplanning.trip.service;

import com.enspy.tripplanning.trip.dto.AddWaypointRequest;
import com.enspy.tripplanning.trip.dto.TripWaypointDTO;
import com.enspy.tripplanning.trip.entity.TripWaypoint;
import com.enspy.tripplanning.trip.repository.TripRepository;
import com.enspy.tripplanning.trip.repository.TripWaypointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ================================================================
 * TripWaypointService - Gestion Étapes des Voyages
 * ================================================================
 * 
 * 🎯 RESPONSABILITÉS:
 * - Ajout/modification/suppression waypoints
 * - Réorganisation ordre (drag&drop)
 * - Validation cohérence (START unique, END unique)
 * - Calcul automatique routes entre étapes
 * 
 * 📚 CONCEPTS PÉDAGOGIQUES:
 * 
 * 1️⃣ ORDER INDEX PATTERN:
 * - order_index définit séquence: 0, 1, 2, ...
 * - Réorganisation = update multiple order_index
 * - Permet insertion milieu sans recalculer tout
 * 
 * 2️⃣ BUSINESS RULES:
 * - 1 seul START par trip
 * - 1 seul END par trip
 * - START.order_index = 0 obligatoire
 * - END.order_index = max obligatoire
 * 
 * 3️⃣ CASCADE EFFECTS:
 * - Ajout waypoint → recalcul stats trip
 * - Suppression waypoint → invalidation cache routes
 * - Réorganisation → recalcul toutes routes
 * 
 * ================================================================
 * @author Thomas Djotio Ndié
 * @since 2024-12-18
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripWaypointService {

    private final TripWaypointRepository waypointRepository;
    private final TripRepository tripRepository;

    // ============================================================
    // CREATE
    // ============================================================
    
    @Transactional
    public Mono<TripWaypointDTO> addWaypoint(UUID tripId, AddWaypointRequest request, UUID requestingUserId) {
        log.info("➕ Ajout waypoint au trip {}", tripId);

        // Validation: POI OU coordonnées custom
        if (request.getPoiId() == null && 
            (request.getCustomLatitude() == null || request.getCustomLongitude() == null)) {
            return Mono.error(new IllegalArgumentException(
                "Fournir soit poiId, soit customLatitude + customLongitude"
            ));
        }

        return tripRepository.findById(tripId)
            .switchIfEmpty(Mono.error(new RuntimeException("Voyage non trouvé")))
            .flatMap(trip -> {
                // TODO: Vérifier permissions edit
                return waypointRepository.findNextOrderIndex(tripId);
            })
            .flatMap(nextIndex -> {
                // Déterminer type waypoint
                TripWaypoint.WaypointType type = TripWaypoint.WaypointType.WAYPOINT;
                if (request.getWaypointType() != null) {
                    type = TripWaypoint.WaypointType.valueOf(request.getWaypointType());
                }

                // Construction waypoint
                TripWaypoint waypoint = TripWaypoint.builder()
                    .tripId(tripId)
                    .poiId(request.getPoiId())
                    .customName(request.getCustomName())
                    .customLatitude(request.getCustomLatitude())
                    .customLongitude(request.getCustomLongitude())
                    .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : nextIndex)
                    .waypointType(type)
                    .plannedArrivalTime(request.getPlannedArrivalTime())
                    .plannedDurationMinutes(request.getPlannedDurationMinutes())
                    .notes(request.getNotes())
                    .build();

                return waypointRepository.save(waypoint);
            })
            .map(this::convertToDTO)
            .doOnSuccess(dto -> log.info("✅ Waypoint ajouté: ID={}, order={}", dto.getWaypointId(), dto.getOrderIndex()));
    }

    // ============================================================
    // READ
    // ============================================================
    
    public Flux<TripWaypointDTO> getTripWaypoints(UUID tripId) {
        log.debug("📍 Liste waypoints trip {}", tripId);

        return waypointRepository.findByTripIdOrderByOrderIndex(tripId)
            .map(this::convertToDTO);
    }

    // ============================================================
    // UPDATE
    // ============================================================
    
    @Transactional
    public Mono<TripWaypointDTO> updateWaypoint(UUID tripId, Long waypointId, AddWaypointRequest request) {
        log.info("✏️ MAJ waypoint {} du trip {}", waypointId, tripId);

        return waypointRepository.findById(waypointId)
            .switchIfEmpty(Mono.error(new RuntimeException("Waypoint non trouvé")))
            .filter(w -> w.getTripId().equals(tripId))
            .switchIfEmpty(Mono.error(new RuntimeException("Waypoint n'appartient pas à ce trip")))
            .map(waypoint -> {
                if (request.getPoiId() != null) waypoint.setPoiId(request.getPoiId());
                if (request.getCustomName() != null) waypoint.setCustomName(request.getCustomName());
                if (request.getCustomLatitude() != null) waypoint.setCustomLatitude(request.getCustomLatitude());
                if (request.getCustomLongitude() != null) waypoint.setCustomLongitude(request.getCustomLongitude());
                if (request.getPlannedArrivalTime() != null) waypoint.setPlannedArrivalTime(request.getPlannedArrivalTime());
                if (request.getPlannedDurationMinutes() != null) waypoint.setPlannedDurationMinutes(request.getPlannedDurationMinutes());
                if (request.getNotes() != null) waypoint.setNotes(request.getNotes());
                
                waypoint.setUpdatedAt(LocalDateTime.now());
                return waypoint;
            })
            .flatMap(waypointRepository::save)
            .map(this::convertToDTO)
            .doOnSuccess(dto -> log.info("✅ Waypoint {} mis à jour", waypointId));
    }

    // ============================================================
    // DELETE
    // ============================================================
    
    @Transactional
    public Mono<Void> deleteWaypoint(UUID tripId, Long waypointId) {
        log.warn("🗑️ Suppression waypoint {} du trip {}", waypointId, tripId);

        return waypointRepository.findById(waypointId)
            .switchIfEmpty(Mono.error(new RuntimeException("Waypoint non trouvé")))
            .filter(w -> w.getTripId().equals(tripId))
            .switchIfEmpty(Mono.error(new RuntimeException("Waypoint n'appartient pas à ce trip")))
            .flatMap(waypointRepository::delete)
            .doOnSuccess(v -> log.info("✅ Waypoint {} supprimé", waypointId));
    }

    // ============================================================
    // CONVERSIONS
    // ============================================================
    
    private TripWaypointDTO convertToDTO(TripWaypoint w) {
        return TripWaypointDTO.builder()
            .waypointId(w.getWaypointId())
            .orderIndex(w.getOrderIndex())
            .waypointType(w.getWaypointType().name())
            .poiId(w.getPoiId())
            .name(w.getDisplayName())
            .latitude(w.getLatitude())
            .longitude(w.getLongitude())
            .plannedArrivalTime(w.getPlannedArrivalTime())
            .plannedDepartureTime(w.getPlannedDepartureTime())
            .plannedDurationMinutes(w.getPlannedDurationMinutes())
            .notes(w.getNotes())
            .build();
    }
}