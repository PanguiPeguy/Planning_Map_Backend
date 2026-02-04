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
public interface TripMemberRepository extends R2dbcRepository<TripMember, UUID> {

        /**
         * Trouve tous les membres d'un voyage
         * 
         * @param tripId ID du voyage
         * @return Flux de membres
         */
        Flux<TripMember> findByTripId(UUID tripId);

        /**
         * Trouve tous les voyages d'un membre
         * 
         * @param userId ID de l'utilisateur
         * @return Flux de memberships
         */
        Flux<TripMember> findByUserId(UUID userId);

        /**
         * Trouve un membre spécifique d'un voyage
         * 
         * @param tripId ID du voyage
         * @param userId ID de l'utilisateur
         * @return Membre si trouvé
         */
        Mono<TripMember> findByTripIdAndUserId(UUID tripId, UUID userId);

        /**
         * Vérifie si un utilisateur est membre d'un voyage
         * 
         * @param tripId ID du voyage
         * @param userId ID de l'utilisateur
         * @return true si membre
         */
        @Query("SELECT EXISTS(SELECT 1 FROM trip_members WHERE trip_id = :tripId AND user_id = :userId)")
        Mono<Boolean> isMember(
                        @Param("tripId") UUID tripId,
                        @Param("userId") UUID userId);

        /**
         * Compte les membres d'un voyage
         * 
         * @param tripId ID du voyage
         * @return Nombre de membres
         */
        Mono<Long> countByTripId(UUID tripId);

        /**
         * Trouve les membres par rôle
         * 
         * @param tripId ID du voyage
         * @param role   Rôle recherché
         * @return Flux de membres
         */
        @Query("SELECT * FROM trip_members WHERE trip_id = :tripId AND role = CAST(:role AS member_role)")
        Flux<TripMember> findByTripIdAndRole(
                        @Param("tripId") UUID tripId,
                        @Param("role") String role);

        /**
         * Supprime un membre d'un voyage
         * 
         * @param tripId ID du voyage
         * @param userId ID de l'utilisateur
         * @return Nombre de lignes supprimées
         */
        @Query("DELETE FROM trip_members WHERE trip_id = :tripId AND user_id = :userId")
        Mono<Long> deleteByTripIdAndUserId(
                        @Param("tripId") UUID tripId,
                        @Param("userId") UUID userId);

        /**
         * Supprime tous les membres d'un voyage
         * 
         * @param tripId ID du voyage
         * @return Nombre de membres supprimés
         */
        @Query("DELETE FROM trip_members WHERE trip_id = :tripId")
        Mono<Long> deleteByTripId(@Param("tripId") UUID tripId);

        /**
         * Trouve les éditeurs d'un voyage
         * 
         * @param tripId ID du voyage
         * @return Flux d'éditeurs
         */
        @Query("SELECT * FROM trip_members WHERE trip_id = :tripId AND role IN ('OWNER', 'EDITOR')")
        Flux<TripMember> findEditorsAndOwner(@Param("tripId") UUID tripId);
}