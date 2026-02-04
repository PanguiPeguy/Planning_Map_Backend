package com.enspy.tripplanning.statistics.service;

import com.enspy.tripplanning.poi.repository.PoiCategoryRepository;
import com.enspy.tripplanning.poi.repository.PoiRepository;
import com.enspy.tripplanning.poi.repository.PoiLikeRepository;
import com.enspy.tripplanning.poi.repository.PoiFavoriteRepository;
import com.enspy.tripplanning.statistics.dto.DashboardStatsDTO;
import com.enspy.tripplanning.trip.repository.TripRepository;
import com.enspy.tripplanning.authentification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
// import removed
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

        private final UserRepository userRepository;
        private final PoiRepository poiRepository;
        private final TripRepository tripRepository;
        private final PoiCategoryRepository categoryRepository;
        private final PoiLikeRepository likeRepository;
        private final PoiFavoriteRepository favoriteRepository;

        public Mono<DashboardStatsDTO> getDashboardStats() {
                log.info("Récupération des statistiques dashboard");

                // Compter les totaux
                Mono<Long> totalUsers = userRepository.count();
                Mono<Long> totalPois = poiRepository.count();
                Mono<Long> totalTrips = tripRepository.count();
                Mono<Long> totalCategories = categoryRepository.count();

                // POI par catégorie
                Mono<Map<String, Long>> poisByCategory = categoryRepository.findAll()
                                .flatMap(category -> poiRepository.countByCategoryId(category.getCategoryId())
                                                .map(count -> (Map.Entry<String, Long>) new java.util.AbstractMap.SimpleEntry<>(
                                                                category.getName(), count)))
                                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                                .defaultIfEmpty(Map.of());

                // Top POI par rating
                Mono<List<DashboardStatsDTO.TopPoiDTO>> topPois = poiRepository
                                .findTop5ByOrderByRatingDescReviewCountDesc()
                                .flatMap(poi -> categoryRepository.findById(poi.getCategoryId())
                                                .map(category -> DashboardStatsDTO.TopPoiDTO.builder()
                                                                .poiId(poi.getPoiId())
                                                                .name(poi.getName())
                                                                .categoryName(category.getName())
                                                                .rating(poi.getRating().doubleValue())
                                                                .reviewCount(poi.getReviewCount())
                                                                .build()))
                                .collectList();

                // Trips récents
                Mono<List<DashboardStatsDTO.RecentTripDTO>> recentTrips = tripRepository
                                .findTop5ByOrderByCreatedAtDesc()
                                .filter(trip -> trip.getOwnerUserId() != null) // Filter trips with null owner
                                .flatMap(trip -> userRepository.findById(trip.getOwnerUserId())
                                                .defaultIfEmpty(com.enspy.tripplanning.authentification.entity.User
                                                                .builder().username("Inconnu").build()) // Handle
                                                                                                        // missing user
                                                .map(user -> DashboardStatsDTO.RecentTripDTO.builder()
                                                                .tripId(trip.getTripId().toString())
                                                                .title(trip.getTitle())
                                                                .username(user.getUsername())
                                                                .status(trip.getStatus().toString())
                                                                .createdAt(trip.getCreatedAt().format(
                                                                                DateTimeFormatter.ISO_DATE_TIME))
                                                                .build()))
                                .collectList();

                // Combiner toutes les statistiques
                return Mono.zip(totalUsers, totalPois, totalTrips, totalCategories, poisByCategory, topPois,
                                recentTrips)
                                .map(tuple -> DashboardStatsDTO.builder()
                                                .totalUsers(tuple.getT1())
                                                .totalPois(tuple.getT2())
                                                .totalTrips(tuple.getT3())
                                                .totalCategories(tuple.getT4())
                                                .poisByCategory(tuple.getT5())
                                                .topPois(tuple.getT6())
                                                .recentTrips(tuple.getT7())
                                                .build())
                                .doOnSuccess(stats -> log.info("Statistiques récupérées: {} users, {} POIs, {} trips",
                                                stats.getTotalUsers(), stats.getTotalPois(), stats.getTotalTrips()));
        }

        public Mono<DashboardStatsDTO> getUserStats(String userIdStr) {
                log.info("Récupération des statistiques pour l'utilisateur: {}", userIdStr);

                java.util.UUID userId;
                try {
                        userId = java.util.UUID.fromString(userIdStr);
                } catch (IllegalArgumentException e) {
                        log.warn("L'identifiant fourni n'est pas un UUID valide, tentative de résolution via email: {}",
                                        userIdStr);
                        // Si c'est un email, on pourrait essayer de chercher le user par email
                        return userRepository.findByEmail(userIdStr)
                                        .flatMap(user -> {
                                                log.info("Utilisateur trouvé pour l'email {}, UUID: {}", userIdStr,
                                                                user.getUserId());
                                                return getUserStats(user.getUserId().toString());
                                        })
                                        .switchIfEmpty(Mono.error(new RuntimeException(
                                                        "Utilisateur non trouvé pour l'identifiant: " + userIdStr)));
                }

                // Trips de l'utilisateur
                Mono<Long> userTrips = tripRepository.countByOwnerUserId(userId)
                                .doOnNext(c -> log.debug("userTrips count: {}", c))
                                .doOnError(e -> log.error("Error in userTrips query", e));

                // POI likés par l'utilisateur
                Mono<Long> likedPoisCount = likeRepository.countByUserId(userId)
                                .doOnNext(c -> log.debug("likedPoisCount: {}", c))
                                .doOnError(e -> log.error("Error in likedPoisCount query", e));

                // POI mis en favoris par l'utilisateur
                Mono<Long> favoritedPoisCount = favoriteRepository.countByUserId(userId)
                                .doOnNext(c -> log.debug("favoritedPoisCount: {}", c))
                                .doOnError(e -> log.error("Error in favoritedPoisCount query", e));

                // Distribution des POI favoris par catégorie - CORRIGÉ
                // Au lieu d'utiliser une projection, on récupère les favoris puis on regroupe
                // manuellement
                Mono<Map<String, Long>> favoritedByCategory = favoriteRepository
                                .findByUserId(userId) // Récupère tous les favoris de l'utilisateur
                                .doOnNext(fav -> log.debug("Favorite POI ID: {}", fav.getPoiId()))
                                .flatMap(favorite -> poiRepository.findById(favorite.getPoiId())
                                                .flatMap(poi -> categoryRepository.findById(poi.getCategoryId())
                                                                .map(category -> category.getName())))
                                .groupBy(categoryName -> categoryName) // Grouper par nom de catégorie
                                .flatMap(group -> group.count()
                                                .map(count -> new java.util.AbstractMap.SimpleEntry<>(group.key(),
                                                                count)))
                                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                                .doOnError(e -> log.error("Error in favoritedByCategory pipe", e))
                                .defaultIfEmpty(Map.of());

                // Top POI favoris de l'utilisateur ordonnés par likes globaux
                Mono<List<DashboardStatsDTO.TopPoiDTO>> topPois = poiRepository
                                .findTopFavoritedByUserIdOrderByLikes(userId)
                                .doOnNext(p -> log.debug("TopPoi favorited: {}", p.getName()))
                                .flatMap(poi -> categoryRepository.findById(poi.getCategoryId())
                                                .map(category -> DashboardStatsDTO.TopPoiDTO.builder()
                                                                .poiId(poi.getPoiId())
                                                                .name(poi.getName())
                                                                .categoryName(category.getName())
                                                                .rating(poi.getRating().doubleValue())
                                                                .reviewCount(poi.getReviewCount())
                                                                .build()))
                                .collectList()
                                .doOnError(e -> log.error("Error in topPois pipe", e))
                                .defaultIfEmpty(List.of());

                Mono<Long> totalCategories = categoryRepository.count()
                                .doOnNext(c -> log.debug("totalCategories: {}", c));

                log.info("Zipping all statistics for user: {}", userId);
                return Mono.zip(userTrips, likedPoisCount, favoritedPoisCount, favoritedByCategory, topPois,
                                totalCategories)
                                .map(tuple -> DashboardStatsDTO.builder()
                                                .totalTrips(tuple.getT1())
                                                .totalLikedPois(tuple.getT2())
                                                .totalFavoritedPois(tuple.getT3())
                                                .poisByCategory(tuple.getT4())
                                                .topPois(tuple.getT5())
                                                .totalCategories(tuple.getT6())
                                                .build())
                                .doOnSuccess(s -> log.info("Successfully stats calculated for user: {}", userIdStr))
                                .doOnError(e -> log.error("Zip failure for user stats", e));
        }
}