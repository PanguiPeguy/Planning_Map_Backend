package com.enspy.tripplanning.trip.service;

import com.enspy.tripplanning.trip.dto.*;
import com.enspy.tripplanning.trip.entity.Trip;
import com.enspy.tripplanning.trip.entity.TripMember;
import com.enspy.tripplanning.trip.entity.TripWaypoint;
import com.enspy.tripplanning.trip.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * TripService - Service Principal Gestion Voyages
 * ================================================================
 * 
 * 🎯 RESPONSABILITÉS:
 * - CRUD voyages complet
 * - Calcul statistiques automatique
 * - Gestion permissions (owner/editor/viewer)
 * - Intégration routing (A*)
 * 
 * 📚 CONCEPTS PÉDAGOGIQUES:
 * 
 * 1️⃣ REACTIVE STREAMS:
 * - Mono<T> = Publisher 0..1 élément
 * - Flux<T> = Publisher 0..N éléments
 * - flatMap = transformation async
 * - zip = combinaison parallèle
 * 
 * 2️⃣ TRANSACTION MANAGEMENT:
 * - @Transactional = atomicité garantie
 * - Rollback auto si exception
 * - Crucial pour opérations multi-tables
 * 
 * 3️⃣ SECURITY BY DESIGN:
 * - Vérification permissions AVANT chaque action
 * - Principe: fail-safe (refuser par défaut)
 * - Never trust user input
 * 
 * ================================================================
 * @author Thomas Djotio Ndié
 * @since 2024-12-18
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripWaypointRepository waypointRepository;
    private final TripMemberRepository memberRepository;

    // ============================================================
    // CREATE
    // ============================================================
    
    @Transactional
    public Mono<TripResponse> createTrip(CreateTripRequest request, UUID ownerUserId) {
        log.info("📝 Création voyage: '{}' par user {}", request.getTitle(), ownerUserId);

        // Validation dates
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                return Mono.error(new IllegalArgumentException(
                    "Date fin doit être après date début"
                ));
            }
        }

        // Construction Trip
        Trip trip = Trip.builder()
            .ownerUserId(ownerUserId)
            .title(request.getTitle())
            .description(request.getDescription())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .status(Trip.TripStatus.DRAFT)
            .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
            .isCollaborative(request.getIsCollaborative() != null ? request.getIsCollaborative() : false)
            .build();

        // Metadata
        if (request.getMetadata() != null) {
            trip.setMetadata(request.getMetadata());
            trip.serializeMetadata();
        }

        // Token partage
        trip.generateShareToken();

        // Sauvegarde Trip + Création TripMember OWNER
        return tripRepository.save(trip)
            .flatMap(savedTrip -> {
                TripMember ownerMember = TripMember.builder()
                    .tripId(savedTrip.getTripId())
                    .userId(ownerUserId)
                    .role(TripMember.MemberRole.OWNER)
                    .notificationsEnabled(true)
                    .build();

                return memberRepository.save(ownerMember)
                    .thenReturn(savedTrip);
            })
            .flatMap(this::enrichTripResponse)
            .doOnSuccess(response -> 
                log.info("✅ Voyage créé: ID={}", response.getTripId())
            );
    }

    // ============================================================
    // READ
    // ============================================================
    
    public Mono<TripResponse> getTripById(UUID tripId, UUID requestingUserId) {
        log.debug("🔍 Récupération trip {}", tripId);

        return tripRepository.findById(tripId)
            .switchIfEmpty(Mono.error(new RuntimeException("Voyage non trouvé: " + tripId)))
            .flatMap(trip -> checkReadPermission(trip, requestingUserId).thenReturn(trip))
            .flatMap(this::enrichTripResponse);
    }

    public Flux<TripResponse> getUserTrips(UUID userId) {
        log.debug("📋 Liste voyages user {}", userId);

        Flux<Trip> ownedTrips = tripRepository.findByOwnerUserId(userId);
        Flux<Trip> sharedTrips = tripRepository.findTripsByMembership(userId);

        return Flux.merge(ownedTrips, sharedTrips)
            .distinct(Trip::getTripId)
            .sort((t1, t2) -> t2.getUpdatedAt().compareTo(t1.getUpdatedAt()))
            .flatMap(this::enrichTripResponse);
    }

    public Mono<TripResponse> getTripByShareToken(String shareToken) {
        log.debug("🔗 Accès trip via token: {}", shareToken);

        return tripRepository.findByShareToken(shareToken)
            .switchIfEmpty(Mono.error(new RuntimeException("Token invalide")))
            .filter(Trip::getIsPublic)
            .switchIfEmpty(Mono.error(new RuntimeException("Voyage non public")))
            .flatMap(this::enrichTripResponse);
    }

    // ============================================================
    // UPDATE
    // ============================================================
    
    @Transactional
    public Mono<TripResponse> updateTrip(UUID tripId, UpdateTripRequest request, UUID requestingUserId) {
        log.info("✏️ MAJ trip {} par user {}", tripId, requestingUserId);

        return tripRepository.findById(tripId)
            .switchIfEmpty(Mono.error(new RuntimeException("Voyage non trouvé")))
            .flatMap(trip -> checkEditPermission(trip, requestingUserId).thenReturn(trip))
            .map(trip -> {
                if (request.getTitle() != null) trip.setTitle(request.getTitle());
                if (request.getDescription() != null) trip.setDescription(request.getDescription());
                if (request.getStartDate() != null) trip.setStartDate(request.getStartDate());
                if (request.getEndDate() != null) trip.setEndDate(request.getEndDate());
                if (request.getStatus() != null) trip.setStatus(Trip.TripStatus.valueOf(request.getStatus()));
                if (request.getIsPublic() != null) trip.setIsPublic(request.getIsPublic());
                if (request.getIsCollaborative() != null) trip.setIsCollaborative(request.getIsCollaborative());
                
                trip.setUpdatedAt(LocalDateTime.now());
                return trip;
            })
            .flatMap(tripRepository::save)
            .flatMap(this::enrichTripResponse)
            .doOnSuccess(r -> log.info("✅ Trip {} mis à jour", tripId));
    }

    // ============================================================
    // DELETE
    // ============================================================
    
    @Transactional
    public Mono<Void> deleteTrip(UUID tripId, UUID requestingUserId) {
        log.warn("🗑️ Suppression trip {} par user {}", tripId, requestingUserId);

        return tripRepository.findById(tripId)
            .switchIfEmpty(Mono.error(new RuntimeException("Voyage non trouvé")))
            .flatMap(trip -> {
                if (!trip.isOwner(requestingUserId)) {
                    return Mono.error(new RuntimeException("Seul owner peut supprimer"));
                }
                return Mono.just(trip);
            })
            .flatMap(trip -> waypointRepository.deleteByTripId(tripId).thenReturn(trip))
            .flatMap(trip -> memberRepository.deleteByTripId(tripId).thenReturn(trip))
            .flatMap(tripRepository::delete)
            .doOnSuccess(v -> log.info("✅ Trip {} supprimé", tripId));
    }

    // ============================================================
    // STATISTIQUES
    // ============================================================
    
    public Mono<TripStatistics> calculateTripStatistics(UUID tripId) {
        return Mono.zip(
            tripRepository.findById(tripId),
            waypointRepository.findByTripIdOrderByOrderIndex(tripId).collectList(),
            memberRepository.countByTripId(tripId)
        ).map(tuple -> {
            Trip trip = tuple.getT1();
            List<TripWaypoint> waypoints = tuple.getT2();
            Long memberCount = tuple.getT3();

            if (waypoints.size() < 2) {
                return TripStatistics.builder()
                    .totalDistanceKm(BigDecimal.ZERO)
                    .totalDurationMinutes(0)
                    .formattedDuration("0 min")
                    .estimatedCost(BigDecimal.ZERO)
                    .waypointCount(waypoints.size())
                    .memberCount(memberCount.intValue())
                    .durationDays(calculateDurationDays(trip))
                    .build();
            }

            // Calcul distances entre waypoints consécutifs
            BigDecimal totalDistance = BigDecimal.ZERO;
            int totalDuration = 0;

            for (int i = 0; i < waypoints.size() - 1; i++) {
                TripWaypoint from = waypoints.get(i);
                TripWaypoint to = waypoints.get(i + 1);
                
                double distKm = calculateHaversineDistance(from, to);
                int durMin = (int) (distKm / 80.0 * 60); // 80 km/h moyen
                
                totalDistance = totalDistance.add(BigDecimal.valueOf(distKm));
                totalDuration += durMin;
            }

            BigDecimal estimatedCost = totalDistance.multiply(new BigDecimal("100")); // 100 FCFA/km

            return TripStatistics.builder()
                .totalDistanceKm(totalDistance)
                .totalDurationMinutes(totalDuration)
                .formattedDuration(formatDuration(totalDuration))
                .estimatedCost(estimatedCost)
                .waypointCount(waypoints.size())
                .memberCount(memberCount.intValue())
                .durationDays(calculateDurationDays(trip))
                .build();
        });
    }

    private double calculateHaversineDistance(TripWaypoint from, TripWaypoint to) {
        double lat1 = Math.toRadians(from.getLatitude().doubleValue());
        double lon1 = Math.toRadians(from.getLongitude().doubleValue());
        double lat2 = Math.toRadians(to.getLatitude().doubleValue());
        double lon2 = Math.toRadians(to.getLongitude().doubleValue());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c; // Rayon Terre = 6371 km
    }

    private Long calculateDurationDays(Trip trip) {
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            return ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
        }
        return null;
    }

    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        
        if (hours > 0 && mins > 0) {
            return hours + "h " + mins + "min";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return mins + "min";
        }
    }

    // ============================================================
    // PERMISSIONS
    // ============================================================
    
    private Mono<Void> checkReadPermission(Trip trip, UUID userId) {
        if (trip.isOwner(userId) || trip.getIsPublic()) {
            return Mono.empty();
        }
        
        return memberRepository.isMember(trip.getTripId(), userId)
            .flatMap(isMember -> {
                if (isMember) return Mono.empty();
                return Mono.error(new RuntimeException("Accès refusé"));
            });
    }

    private Mono<Object> checkEditPermission(Trip trip, UUID userId) {
        if (trip.isOwner(userId)) {
            return Mono.empty();
        }

        return memberRepository.findByTripIdAndUserId(trip.getTripId(), userId)
            .flatMap(member -> {
                if (member.canEdit()) return Mono.empty();
                return Mono.error(new RuntimeException("Permission refusée (rôle: " + member.getRole() + ")"));
            })
            .switchIfEmpty(Mono.error(new RuntimeException("Vous n'êtes pas membre")));
    }

    // ============================================================
    // ENRICHISSEMENT DTO
    // ============================================================
    
    private Mono<TripResponse> enrichTripResponse(Trip trip) {
        return Mono.zip(
            waypointRepository.findByTripIdOrderByOrderIndex(trip.getTripId())
                .map(this::convertWaypointToDTO)
                .collectList(),
            memberRepository.findByTripId(trip.getTripId())
                .map(this::convertMemberToDTO)
                .collectList(),
            calculateTripStatistics(trip.getTripId())
        ).map(tuple -> {
            trip.deserializeMetadata();

            return TripResponse.builder()
                .tripId(trip.getTripId())
                .title(trip.getTitle())
                .description(trip.getDescription())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .status(trip.getStatus().name())
                .isPublic(trip.getIsPublic())
                .isCollaborative(trip.getIsCollaborative())
                .shareToken(trip.getShareToken())
                .statistics(tuple.getT3())
                .owner(TripOwnerInfo.builder()
                    .userId(trip.getOwnerUserId())
                    .build())
                .waypoints(tuple.getT1())
                .members(tuple.getT2())
                .metadata(trip.getMetadata())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
        });
    }

    private TripWaypointDTO convertWaypointToDTO(TripWaypoint w) {
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

    private TripMemberDTO convertMemberToDTO(TripMember m) {
        return TripMemberDTO.builder()
            .userId(m.getUserId())
            .role(m.getRole().name())
            .joinedAt(m.getJoinedAt())
            .lastActivityAt(m.getLastActivityAt())
            .notificationsEnabled(m.getNotificationsEnabled())
            .build();
    }
}