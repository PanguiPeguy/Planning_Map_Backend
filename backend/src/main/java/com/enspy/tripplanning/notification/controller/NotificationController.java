package com.enspy.tripplanning.notification.controller;

import com.enspy.tripplanning.notification.entity.Notification;
import com.enspy.tripplanning.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestion notifications utilisateur")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
        summary = "Lister mes notifications",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping
    public Flux<Notification> getMyNotifications(Authentication auth) {
        UUID userId = extractUserId(auth);
        return notificationService.getAllNotifications(userId);
    }

    @Operation(
        summary = "Lister notifications non lues",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/unread")
    public Flux<Notification> getUnreadNotifications(Authentication auth) {
        UUID userId = extractUserId(auth);
        return notificationService.getUnreadNotifications(userId);
    }

    @Operation(
        summary = "Compter notifications non lues",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/unread/count")
    public Mono<Long> countUnread(Authentication auth) {
        UUID userId = extractUserId(auth);
        return notificationService.countUnread(userId);
    }

    @Operation(
        summary = "Marquer notification comme lue",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PutMapping("/{notificationId}/read")
    public Mono<Notification> markAsRead(
        @PathVariable UUID notificationId,
        Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        return notificationService.markAsRead(notificationId, userId);
    }

    @Operation(
        summary = "Marquer toutes comme lues",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> markAllAsRead(Authentication auth) {
        UUID userId = extractUserId(auth);
        return notificationService.markAllAsRead(userId);
    }

    @Operation(
        summary = "Supprimer une notification",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteNotification(
        @PathVariable UUID notificationId,
        Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        return notificationService.deleteNotification(notificationId, userId);
    }

    private UUID extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Non authentifié");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof com.enspy.tripplanning.authentification.entity.User) {
            return ((com.enspy.tripplanning.authentification.entity.User) principal).getUserId();
        }
        
        throw new RuntimeException("Type principal invalide");
    }
}