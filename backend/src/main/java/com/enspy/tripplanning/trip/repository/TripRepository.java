package com.enspy.tripplanning.trip.repository;

import com.enspy.tripplanning.trip.entity.Trip;
import com.enspy.tripplanning.trip.entity.TripWaypoint;
import com.enspy.tripplanning.trip.entity.TripMember;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface TripRepository extends R2dbcRepository<Trip, UUID> {

        /**
         * Trouve tous les voyages d'un utilisateur
         * 
         * @param ownerUserId ID du propriétaire
         * @return Flux de voyages
         */
        Flux<Trip> findByOwnerUserId(UUID ownerUserId);

        /**
         * Trouve tous les voyages publics
         * 
         * @return Flux de voyages publics
         */
        @Query("SELECT * FROM trips WHERE is_public = TRUE ORDER BY created_at DESC")
        Flux<Trip> findAllPublicTrips();

        /**
         * Trouve un voyage par son token de partage
         * 
         * @param shareToken Token unique
         * @return Voyage si trouvé
         */
        Mono<Trip> findByShareToken(String shareToken);

        /**
         * Trouve les voyages par statut
         * 
         * @param status Statut du voyage
         * @return Flux de voyages
         */
        @Query("SELECT * FROM trips WHERE status = CAST(:status AS trip_status)")
        Flux<Trip> findByStatus(@Param("status") String status);

        /**
         * Trouve les voyages d'un utilisateur avec un statut spécifique
         * 
         * @param ownerUserId ID du propriétaire
         * @param status      Statut
         * @return Flux de voyages
         */
        @Query("SELECT * FROM trips WHERE owner_user_id = :ownerUserId AND status = CAST(:status AS trip_status)")
        Flux<Trip> findByOwnerUserIdAndStatus(
                        @Param("ownerUserId") UUID ownerUserId,
                        @Param("status") String status);

        /**
         * Trouve les voyages où l'utilisateur est membre
         * 
         * @param userId ID de l'utilisateur
         * @return Flux de voyages
         */
        @Query("""
                        SELECT t.* FROM trips t
                        JOIN trip_members tm ON t.trip_id = tm.trip_id
                        WHERE tm.user_id = :userId
                        ORDER BY t.updated_at DESC
                        """)
        Flux<Trip> findTripsByMembership(@Param("userId") UUID userId);

        /**
         * Compte les voyages d'un utilisateur
         * 
         * @param ownerUserId ID du propriétaire
         * @return Nombre de voyages
         */
        Mono<Long> countByOwnerUserId(UUID ownerUserId);

        /**
         * Vérifie si un utilisateur est propriétaire d'un voyage
         * 
         * @param tripId ID du voyage
         * @param userId ID de l'utilisateur
         * @return true si propriétaire
         */
        @Query("SELECT EXISTS(SELECT 1 FROM trips WHERE trip_id = :tripId AND owner_user_id = :userId)")
        Mono<Boolean> isUserOwner(
                        @Param("tripId") UUID tripId,
                        @Param("userId") UUID userId);

        /**
         * Trouve les 5 trips les plus récents (tous utilisateurs)
         * Pour dashboard admin
         * 
         * @return Flux des 5 derniers trips
         */
        @Query("SELECT * FROM trips ORDER BY created_at DESC LIMIT 5")
        Flux<Trip> findTop5ByOrderByCreatedAtDesc();

}
