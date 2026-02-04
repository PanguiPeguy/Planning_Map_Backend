package com.enspy.tripplanning.routing.repository;

import com.enspy.tripplanning.routing.model.CalculatedRoute;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface CalculatedRouteRepository extends R2dbcRepository<CalculatedRoute, UUID> {

        @Query("""
                        SELECT * FROM calculated_routes
                        WHERE from_waypoint_id = :from
                          AND to_waypoint_id = :to
                          AND is_cached = TRUE
                          AND cache_expires_at > NOW()
                        ORDER BY created_at DESC
                        LIMIT 1
                        """)
        Mono<CalculatedRoute> findCachedRoute(
                        @Param("from") Long fromWaypointId,
                        @Param("to") Long toWaypointId);

        @Query("UPDATE calculated_routes SET is_cached = FALSE WHERE trip_id = :tripId")
        Mono<Void> invalidateCacheForTrip(@Param("tripId") UUID tripId);

        @Query("DELETE FROM calculated_routes WHERE cache_expires_at < NOW()")
        Mono<Long> cleanExpiredCache();
}