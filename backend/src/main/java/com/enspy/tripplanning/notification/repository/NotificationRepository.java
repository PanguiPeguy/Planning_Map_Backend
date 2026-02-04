package com.enspy.tripplanning.notification.repository;

import com.enspy.tripplanning.notification.entity.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface NotificationRepository extends R2dbcRepository<Notification, UUID> {

    Flux<Notification> findByUserId(UUID userId);

    Flux<Notification> findByUserIdAndIsReadFalse(UUID userId);

    Mono<Long> countByUserIdAndIsReadFalse(UUID userId);
}