package com.enspy.tripplanning.routing.service;

import com.enspy.tripplanning.routing.model.CalculatedRoute;
import com.enspy.tripplanning.routing.model.Route;
import com.enspy.tripplanning.routing.repository.CalculatedRouteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ================================================================
 * CalculatedRouteService - Cache Intelligent Routes
 * ================================================================
 * 
 * 🎯 OBJECTIFS (Cahier des charges p.7):
 * - Cache routes fréquentes → gain x100 perf
 * - Expiration 24h par défaut
 * - Invalidation si graphe modifié
 * 
 * 📊 PERFORMANCE:
 * - Route en cache : < 50ms (vs 500-2000ms calcul)
 * - Hit rate attendu : 70%
 * 
 * 🔄 STRATÉGIE CACHE:
 * 1. Lookup cache (from/to waypoints)
 * 2. Si hit ET valide → retourner
 * 3. Si miss → calculer A* → sauver cache
 * 
 * ================================================================
 * @author Thomas Djotio Ndié
 * @since 2024-12-18
 * ================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatedRouteService {

    private final CalculatedRouteRepository calculatedRouteRepository;
    private final AStarService aStarService;
    private final ObjectMapper objectMapper;

    /**
     * Récupère route depuis cache OU calcule si absente
     * 
     * @param fromNodeId Nœud départ
     * @param toNodeId Nœud arrivée
     * @param tripId ID voyage (optionnel)
     * @return Route calculée
     */
    public Mono<Route> getOrCalculateRoute(Long fromNodeId, Long toNodeId, UUID tripId) {
        log.debug("🔍 Recherche route en cache: {} → {}", fromNodeId, toNodeId);

        return calculatedRouteRepository
            .findCachedRoute(fromNodeId, toNodeId)
            .filter(CalculatedRoute::isCacheValid)
            .flatMap(this::deserializeRoute)
            .doOnNext(route -> log.info("✓ Route trouvée en cache (gain: {}ms évités)", 
                route.getComputationTimeMs()))
            .switchIfEmpty(Mono.defer(() -> {
                log.info("⚠ Cache miss - Calcul A* nécessaire");
                
                return aStarService.calculateShortestPath(fromNodeId, toNodeId)
                    .flatMap(route -> saveToCache(route, tripId, fromNodeId, toNodeId)
                        .thenReturn(route));
            }));
    }

    /**
     * Sauvegarde route en cache
     */
    private Mono<CalculatedRoute> saveToCache(Route route, UUID tripId, Long fromNodeId, Long toNodeId) {
        try {
            String pathNodesJson = objectMapper.writeValueAsString(
                route.getNodes().stream().map(n -> n.getNodeId()).toList()
            );
            
            String pathEdgesJson = objectMapper.writeValueAsString(
                route.getEdges().stream().map(e -> e.getEdgeId()).toList()
            );

            CalculatedRoute cached = CalculatedRoute.builder()
                .tripId(tripId)
                .fromWaypointId(fromNodeId)
                .toWaypointId(toNodeId)
                .algorithm(CalculatedRoute.RoutingAlgorithm.ASTAR)
                .pathNodesJson(pathNodesJson)
                .pathEdgesJson(pathEdgesJson)
                .totalDistanceMeters(BigDecimal.valueOf(route.getTotalDistanceKm() * 1000))
                .totalDurationSeconds(BigDecimal.valueOf(route.getTotalTimeSeconds()))
                .computationTimeMs(route.getComputationTimeMs().intValue())
                .nodesExplored(route.getNodesExplored())
                .isCached(true)
                .build();

            cached.setCacheExpiration(24); // 24 heures

            return calculatedRouteRepository.save(cached)
                .doOnSuccess(c -> log.info("✓ Route sauvée en cache (ID: {})", c.getRouteId()));
        } catch (Exception e) {
            log.error("❌ Erreur sérialisation route pour cache", e);
            return Mono.empty();
        }
    }

    /**
     * Désérialise route depuis cache
     */
    private Mono<Route> deserializeRoute(CalculatedRoute cached) {
        // TODO: Reconstruire Route depuis pathNodesJson + pathEdgesJson
        // Pour l'instant, retourner route basique
        return Mono.just(Route.builder()
            .totalDistanceKm(cached.getTotalDistanceMeters().doubleValue() / 1000.0)
            .totalTimeSeconds(cached.getTotalDurationSeconds().intValue())
            .found(true)
            .nodesExplored(cached.getNodesExplored())
            .computationTimeMs(cached.getComputationTimeMs().longValue())
            .build());
    }

    /**
     * Invalide cache pour un trip
     */
    public Mono<Void> invalidateCacheForTrip(UUID tripId) {
        log.info("🔄 Invalidation cache pour trip {}", tripId);
        return calculatedRouteRepository.invalidateCacheForTrip(tripId);
    }

    /**
     * Nettoie cache expiré (scheduled task)
     */
    public Mono<Long> cleanExpiredCache() {
        log.info("🧹 Nettoyage cache expiré");
        return calculatedRouteRepository.cleanExpiredCache()
            .doOnSuccess(count -> log.info("✓ {} routes expirées supprimées", count));
    }
}