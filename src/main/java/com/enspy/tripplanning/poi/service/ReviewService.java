package com.enspy.tripplanning.poi.service;

import com.enspy.tripplanning.poi.dto.CreateReviewRequest;
import com.enspy.tripplanning.poi.dto.ReviewDTO;
import com.enspy.tripplanning.poi.entity.Review;
import com.enspy.tripplanning.poi.repository.PoiRepository;
import com.enspy.tripplanning.poi.repository.PoiFavoriteRepository;
import com.enspy.tripplanning.poi.repository.ReviewRepository;
import com.enspy.tripplanning.notification.service.NotificationService;
import com.enspy.tripplanning.authentification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PoiRepository poiRepository;
    private final PoiFavoriteRepository poiFavoriteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Mono<ReviewDTO> createReview(Long poiId, CreateReviewRequest request, UUID userId) {
        log.info("✍️ Création avis POI {} par user {}", poiId, userId);

        return poiRepository.findById(poiId)
                .switchIfEmpty(Mono.error(new RuntimeException("POI non trouvé")))
                .then(reviewRepository.existsByPoiIdAndUserId(poiId, userId))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Vous avez déjà noté ce POI"));
                    }

                    Review review = Review.builder()
                            .poiId(poiId)
                            .userId(userId)
                            .rating(BigDecimal.valueOf(request.getRating()))
                            .comment(request.getComment())
                            .isVerifiedVisit(false)
                            .build();

                    if (request.getImages() != null) {
                        review.setImages(request.getImages().toArray(new String[0]));
                    }

                    return reviewRepository.save(review);
                })
                .flatMap(review -> updatePoiRating(poiId).thenReturn(review))
                .flatMap(review -> {
                    // Récupérer le POI et l'utilisateur pour les notifications
                    return poiRepository.findById(poiId)
                            .zipWith(userRepository.findById(userId))
                            .flatMap(tuple -> {
                                String poiName = tuple.getT1().getName();
                                String commenterName = tuple.getT2().getDisplayName();

                                // Notification aux admins avec le texte du commentaire
                                notificationService
                                        .sendPoiCommentNotificationToAdmins(poiId, poiName, commenterName,
                                                review.getComment())
                                        .subscribe();

                                // Notification aux utilisateurs qui ont ce POI en favori
                                return poiFavoriteRepository.findByPoiId(poiId)
                                        .map(fav -> fav.getUserId())
                                        .collectList()
                                        .doOnSuccess(userIds -> {
                                            if (!userIds.isEmpty()) {
                                                notificationService.sendFavoriteActivityNotification(
                                                        poiId, poiName, commenterName, "COMMENT",
                                                        reactor.core.publisher.Flux.fromIterable(userIds), userId,
                                                        review.getComment())
                                                        .subscribe();
                                            }
                                        })
                                        .thenReturn(review);
                            })
                            .defaultIfEmpty(review);
                })
                .map(this::convertToDTO)
                .doOnSuccess(dto -> log.info("✅ Avis créé: ID={}", dto.getReviewId()));
    }

    public Flux<ReviewDTO> getPoiReviews(Long poiId) {
        return reviewRepository.findByPoiIdOrderByCreatedAtDesc(poiId)
                .flatMap(review -> userRepository.findById(review.getUserId())
                        .map(user -> {
                            ReviewDTO dto = convertToDTO(review);
                            dto.setUsername(user.getUsername());
                            return dto;
                        })
                        .defaultIfEmpty(convertToDTO(review)));
    }

    @Transactional
    public Mono<Void> deleteReview(Long reviewId, UUID userId) {
        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new RuntimeException("Avis non trouvé")))
                .flatMap(review -> {
                    if (!review.getUserId().equals(userId)) {
                        return Mono.error(new RuntimeException("Vous ne pouvez supprimer que vos propres avis"));
                    }
                    return reviewRepository.delete(review)
                            .then(updatePoiRating(review.getPoiId()));
                });
    }

    private Mono<Void> updatePoiRating(Long poiId) {
        return reviewRepository.findByPoiId(poiId)
                .collectList()
                .flatMap(reviews -> {
                    if (reviews.isEmpty()) {
                        return poiRepository.findById(poiId)
                                .flatMap(poi -> {
                                    poi.setRating(BigDecimal.ZERO);
                                    return poiRepository.save(poi);
                                })
                                .then();
                    }

                    double avgRating = reviews.stream()
                            .mapToDouble(r -> r.getRating().doubleValue())
                            .average()
                            .orElse(0.0);

                    return poiRepository.findById(poiId)
                            .flatMap(poi -> {
                                poi.setRating(BigDecimal.valueOf(avgRating));
                                return poiRepository.save(poi);
                            })
                            .then();
                });
    }

    private ReviewDTO convertToDTO(Review review) {
        return ReviewDTO.builder()
                .reviewId(review.getReviewId())
                .poiId(review.getPoiId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .images(review.getImages() != null ? java.util.Arrays.asList(review.getImages()) : null)
                .isVerifiedVisit(review.getIsVerifiedVisit())
                .helpfulCount(review.getHelpfulCount())
                .createdAt(review.getCreatedAt())
                .timeAgo(review.getTimeAgo())
                .build();
    }
}