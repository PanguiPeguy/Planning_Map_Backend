package com.enspy.tripplanning.planning.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("planning_items")
public class PlanningItem implements org.springframework.data.domain.Persistable<UUID> {
    @Id
    @Column("id")
    private UUID id;

    @Column("planning_id")
    private UUID planningId;

    @Column("origin_city")
    private String originCity;

    @Column("destination_city")
    private String destinationCity;

    @Column("planned_date")
    private LocalDate plannedDate;

    @Column("departure_time")
    private LocalTime departureTime;

    @Column("origin_latitude")
    private Double originLatitude;

    @Column("origin_longitude")
    private Double originLongitude;

    @Column("destination_latitude")
    private Double destinationLatitude;

    @Column("destination_longitude")
    private Double destinationLongitude;

    @Column("distance_meters")
    private Double distanceMeters;

    @Column("travel_time_seconds")
    private Integer travelTimeSeconds;

    @Column("status")
    @Builder.Default
    private String status = "PENDING"; // PENDING, CALCULATED

    @Column("route_geom")
    private String routeGeom; // PostGIS LineString or Encoded Polyline (Chosen Route)

    @Column("optimal_route_geom")
    private String optimalRouteGeom; // The calculated "best" route for comparison (Green line)

    @Column("itinerary_id")
    private UUID itineraryId; // Linked saved itinerary if applicable

    @Column("selected_waypoints_json")
    private String selectedWaypointsJson; // JSON array of POI IDs or custom points overrides

    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @org.springframework.data.annotation.Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}
