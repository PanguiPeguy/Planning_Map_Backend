package com.enspy.tripplanning.poi.controller;

import com.enspy.tripplanning.poi.dto.CreateReviewRequest;
import com.enspy.tripplanning.poi.dto.ReviewDTO;
import com.enspy.tripplanning.poi.service.ReviewService;
import com.enspy.tripplanning.authentification.entity.User;
import com.enspy.tripplanning.authentification.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pois/{poiId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Avis sur POI")
public class ReviewController {

    private final ReviewService reviewService;
    private final com.enspy.tripplanning.poi.repository.ReviewRepository reviewRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Créer un avis", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ReviewDTO> createReview(
            @PathVariable Long poiId,
            @Valid @RequestBody CreateReviewRequest request,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        return reviewService.createReview(poiId, request, userId);
    }

    @Operation(summary = "Lister les avis d'un POI")
    @GetMapping
    public Flux<ReviewDTO> getPoiReviews(@PathVariable Long poiId) {
        return reviewService.getPoiReviews(poiId);
    }

    @Operation(summary = "Supprimer mon avis", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteReview(
            @PathVariable Long poiId,
            @PathVariable Long reviewId,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        return reviewService.deleteReview(reviewId, userId);
    }

    private UUID extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Non authentifié");
        }

        Object principal = auth.getPrincipal();

        // 1. Cas standard: Principal est l'entité User complète
        if (principal instanceof User) {
            return ((User) principal).getUserId();
        }

        // 2. Cas fallback: Principal est un String (identifiant/email) venant du token
        if (principal instanceof String && !((String) principal).isBlank()) {
            String token = auth.getCredentials() != null ? auth.getCredentials().toString() : null;
            if (token != null && jwtTokenProvider.validateToken(token)) {
                return jwtTokenProvider.getUserIdFromToken(token);
            }
        }

        throw new RuntimeException("Impossible d'extraire l'ID utilisateur");
    }

    @Operation(summary = "Récupérer le nombre d'avis d'un POI")
    @GetMapping("/count")
    public Mono<Long> getPoiReviewCount(@PathVariable Long poiId) {
        return reviewRepository.countByPoiId(poiId);
    }
}