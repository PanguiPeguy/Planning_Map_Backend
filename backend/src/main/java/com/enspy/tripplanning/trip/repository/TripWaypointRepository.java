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
public interface TripWaypointRepository extends R2dbcRepository<TripWaypoint, Long> {

  /**
   * Trouve tous les waypoints d'un voyage, ordonnés
   * 
   * @param tripId ID du voyage
   * @return Flux de waypoints ordonnés
   */
  @Query("SELECT * FROM trip_waypoints WHERE trip_id = :tripId ORDER BY order_index ASC")
  Flux<TripWaypoint> findByTripIdOrderByOrderIndex(@Param("tripId") UUID tripId);

  /**
   * Trouve le waypoint START d'un voyage
   * 
   * @param tripId ID du voyage
   * @return Waypoint de départ
   */
  @Query("""
      SELECT * FROM trip_waypoints
      WHERE trip_id = :tripId
        AND waypoint_type = 'START'
      LIMIT 1
      """)
  Mono<TripWaypoint> findStartWaypoint(@Param("tripId") UUID tripId);

  /**
   * Trouve le waypoint END d'un voyage
   * 
   * @param tripId ID du voyage
   * @return Waypoint d'arrivée
   */
  @Query("""
      SELECT * FROM trip_waypoints
      WHERE trip_id = :tripId
        AND waypoint_type = 'END'
      LIMIT 1
      """)
  Mono<TripWaypoint> findEndWaypoint(@Param("tripId") UUID tripId);

  /**
   * Compte les waypoints d'un voyage
   * 
   * @param tripId ID du voyage
   * @return Nombre de waypoints
   */
  Mono<Long> countByTripId(UUID tripId);

  /**
   * Trouve le prochain order_index disponible
   * 
   * @param tripId ID du voyage
   * @return Prochain index
   */
  @Query("SELECT COALESCE(MAX(order_index) + 1, 0) FROM trip_waypoints WHERE trip_id = :tripId")
  Mono<Integer> findNextOrderIndex(@Param("tripId") UUID tripId);

  /**
   * Supprime tous les waypoints d'un voyage
   * 
   * @param tripId ID du voyage
   * @return Nombre de waypoints supprimés
   */
  @Query("DELETE FROM trip_waypoints WHERE trip_id = :tripId")
  Mono<Long> deleteByTripId(@Param("tripId") UUID tripId);

  /**
   * Trouve les waypoints utilisant un POI spécifique
   * 
   * @param poiId ID du POI
   * @return Flux de waypoints
   */
  Flux<TripWaypoint> findByPoiId(Long poiId);
}
