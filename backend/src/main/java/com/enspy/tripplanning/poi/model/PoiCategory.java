package com.enspy.tripplanning.poi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("poi_categories")
public class PoiCategory {

    @Id
    @Column("category_id")
    private Long categoryId;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("icon")
    private String icon;

    @Column("color")
    private String color;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
