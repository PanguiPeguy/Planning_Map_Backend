package com.enspy.tripplanning.poi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("pois")
public class Poi {

    @Id
    @Column("poi_id")
    private Long poiId;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("latitude")
    private Double latitude;

    @Column("longitude")
    private Double longitude;

    @Column("category_id")
    private Long categoryId;

    @Column("address_street")
    private String addressStreet;

    @Column("address_city")
    private String addressCity;

    @Column("address_postal_code")
    private String addressPostalCode;

    @Column("address_region")
    private String addressRegion;

    @Column("address_neighborhood")
    private String addressNeighborhood;

    @Column("phone")
    private String phone;

    @Column("rating")
    private Double rating;

    @Column("review_count")
    private Integer reviewCount;

    @Column("opening_hours")
    private String openingHoursJson; // JSON stored as String

    @Column("services")
    private String servicesJson; // JSON stored as String

    @Column("price_range")
    private String priceRange;

    @Column("metadata")
    private String metadataJson; // JSON stored as String

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private PoiCategory category;

    @Transient
    private Map<String, String> openingHours;

    @Transient
    private List<String> services;

    @Transient
    private Map<String, Object> metadata;
}