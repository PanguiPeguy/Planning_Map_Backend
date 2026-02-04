package com.enspy.tripplanning.poi.service;

import com.enspy.tripplanning.poi.entity.PoiFavorite;
import com.enspy.tripplanning.poi.entity.PoiLike;
import com.enspy.tripplanning.poi.repository.PoiFavoriteRepository;
import com.enspy.tripplanning.poi.repository.PoiLikeRepository;
import com.enspy.tripplanning.poi.repository.PoiRepository;
import com.enspy.tripplanning.notification.service.NotificationService;
import com.enspy.tripplanning.authentification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiInteractionService {

    private final PoiLikeRepository poiLikeRepository;
    private final PoiFavoriteRepository poiFavoriteRepository;
    private final PoiRepository poiRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public Mono<Void> likePoi(Long poiId, UUID userId) {
        return poiLikeRepository.existsByPoiIdAndUserId(poiId, userId)
                .flatMap(exists -> {
                    if (exists)
                        return Mono.empty();
                    PoiLike like = PoiLike.builder()
                            .poiId(poiId)
                            .userId(userId)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return poiLikeRepository.save(like)
                            .flatMap(savedLike -> {
                                // Envoyer notifications aux utilisateurs qui ont ce POI en favori
                                return poiRepository.findById(poiId)
                                        .zipWith(userRepository.findById(userId))
                                        .flatMap(tuple -> {
                                            String poiName = tuple.getT1().getName();
                                            String likerName = tuple.getT2().getDisplayName();

                                            return poiFavoriteRepository.findByPoiId(poiId)
                                                    .map(fav -> fav.getUserId())
                                                    .collectList()
                                                    .doOnSuccess(userIds -> {
                                                        if (!userIds.isEmpty()) {
                                                            notificationService.sendFavoriteActivityNotification(
                                                                    poiId, poiName, likerName, "LIKE",
                                                                    Flux.fromIterable(userIds), userId, null)
                                                                    .subscribe();
                                                        }
                                                    })
                                                    .then();
                                        })
                                        .then();
                            })
                            .then();
                });
    }

    public Mono<Void> unlikePoi(Long poiId, UUID userId) {
        return poiLikeRepository.deleteByPoiIdAndUserId(poiId, userId);
    }

    public Mono<Void> addFavorite(Long poiId, UUID userId, String notes) {
        return poiFavoriteRepository.existsByPoiIdAndUserId(poiId, userId)
                .flatMap(exists -> {
                    if (exists)
                        return Mono.empty();
                    PoiFavorite favorite = PoiFavorite.builder()
                            .poiId(poiId)
                            .userId(userId)
                            .notes(notes)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return poiFavoriteRepository.save(favorite).then();
                });
    }

    public Mono<Void> removeFavorite(Long poiId, UUID userId) {
        return poiFavoriteRepository.deleteByPoiIdAndUserId(poiId, userId);
    }

    public Mono<Boolean> isLiked(Long poiId, UUID userId) {
        if (userId == null)
            return Mono.just(false);
        return poiLikeRepository.existsByPoiIdAndUserId(poiId, userId);
    }

    public Mono<Boolean> isFavorite(Long poiId, UUID userId) {
        if (userId == null)
            return Mono.just(false);
        return poiFavoriteRepository.existsByPoiIdAndUserId(poiId, userId);
    }

    public Mono<Long> getLikeCount(Long poiId) {
        return poiLikeRepository.countByPoiId(poiId);
    }

    public Mono<Long> getFavoriteCount(Long poiId) {
        return poiFavoriteRepository.countByPoiId(poiId);
    }

    public Flux<com.enspy.tripplanning.poi.entity.Poi> getFavoritePoisByUserId(UUID userId, Pageable pageable) {
        return poiFavoriteRepository.findFavoritePoisByUserId(userId, pageable);
    }

    public Mono<Long> countFavoriteByUserId(UUID userId) {
        return poiFavoriteRepository.countByUserId(userId);
    }
}
