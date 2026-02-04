package com.enspy.tripplanning.planning.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("plannings")
public class Planning implements org.springframework.data.domain.Persistable<UUID> {
    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("user_id")
    private UUID userId;

    @Column("status")
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, FINALIZED

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
