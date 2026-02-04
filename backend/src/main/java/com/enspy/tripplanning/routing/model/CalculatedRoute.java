package com.enspy.tripplanning.routing.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("calculated_routes")
public class CalculatedRoute {
    @Id
    @Column("route_id")
    private UUID routeId;
    
    @Column("trip_id")
    private UUID tripId;
    
    @Column("from_waypoint_id")
    private Long fromWaypointId;
    
    @Column("to_waypoint_id")
    private Long toWaypointId;
    
    // Coordonnées
    @Column("from_latitude")
    private BigDecimal fromLatitude;
    
    @Column("from_longitude")
    private BigDecimal fromLongitude;
    
    @Column("to_latitude")
    private BigDecimal toLatitude;
    
    @Column("to_longitude")
    private BigDecimal toLongitude;
    
    // Algorithme utilisé
    @Column("algorithm")
    @Builder.Default
    private RoutingAlgorithm algorithm = RoutingAlgorithm.ASTAR;
    
    // Résultat
    @Column("path_nodes")
    private String pathNodesJson;  // [123, 456, 789...]
    
    @Column("path_edges")
    private String pathEdgesJson;  // [234, 567...]
    
    // path_geometry GEOMETRY(LineString) géré par PostGIS
    
    @Column("total_distance_meters")
    private BigDecimal totalDistanceMeters;
    
    @Column("total_duration_seconds")
    private BigDecimal totalDurationSeconds;
    
    // Détails
    @Column("detailed_instructions")
    private String detailedInstructionsJson;
    
    @Column("toll_roads")
    private String tollRoadsJson;
    
    @Column("total_toll_cost")
    @Builder.Default
    private BigDecimal totalTollCost = BigDecimal.ZERO;
    
    // Performance
    @Column("computation_time_ms")
    private Integer computationTimeMs;
    
    @Column("nodes_explored")
    private Integer nodesExplored;
    
    // Cache
    @Column("is_cached")
    @Builder.Default
    private Boolean isCached = true;
    
    @Column("cache_expires_at")
    private LocalDateTime cacheExpiresAt;
    
    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum RoutingAlgorithm { ASTAR, CH, DIJKSTRA }
    
    public boolean isCacheValid() {
        return cacheExpiresAt != null && LocalDateTime.now().isBefore(cacheExpiresAt);
    }
    
    public void setCacheExpiration(int hours) {
        this.cacheExpiresAt = LocalDateTime.now().plusHours(hours);
    }
}