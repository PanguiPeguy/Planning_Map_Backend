package com.enspy.tripplanning.poi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("poi_favorites")
public class PoiFavorite {
    @Id
    @Column("favorite_id")
    private Long favoriteId;

    @Column("poi_id")
    private Long poiId;

    @Column("user_id")
    private UUID userId;

    @Column("notes")
    private String notes;

    @Column("created_at")
    private LocalDateTime createdAt;
}
