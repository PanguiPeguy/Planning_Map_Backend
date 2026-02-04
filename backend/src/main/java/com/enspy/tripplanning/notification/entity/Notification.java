package com.enspy.tripplanning.notification.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("notifications")
public class Notification {
    @Id
    @Column("notification_id")
    private UUID notificationId;
    
    @Column("user_id")
    private UUID userId;
    
    @Column("type")
    private String type;  // TRIP_INVITE, POI_VERIFIED, REVIEW_REPLY
    
    @Column("title")
    private String title;
    
    @Column("message")
    private String message;
    
    @Column("related_entity_type")
    private String relatedEntityType;
    
    @Column("related_entity_id")
    private String relatedEntityId;
    
    @Column("action_url")
    private String actionUrl;
    
    @Column("is_read")
    @Builder.Default
    private Boolean isRead = false;
    
    @Column("read_at")
    private LocalDateTime readAt;
    
    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
