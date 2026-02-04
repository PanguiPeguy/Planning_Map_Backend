package com.enspy.tripplanning.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service générique de cache Redis réactif.
 * 
 * Fournit des méthodes pour:
 * - Mise en cache avec TTL configurable
 * - Récupération depuis cache
 * - Invalidation de cache
 * - Métriques de performance (cache hit/miss)
 * 
 * @author Planning Map Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    // Préfixes pour les clés de cache
    public static final String POI_PREFIX = "poi:";
    public static final String POI_LIST_PREFIX = "poi:list:";
    public static final String ROUTE_PREFIX = "route:";
    public static final String GEOCODE_PREFIX = "geocode:";

    // TTL par défaut
    public static final Duration POI_TTL = Duration.ofHours(1); // 1 heure pour POI individuel
    public static final Duration POI_LIST_TTL = Duration.ofMinutes(5); // 5 minutes pour listes
    public static final Duration ROUTE_TTL = Duration.ofHours(24); // 24 heures pour routes
    public static final Duration GEOCODE_TTL = Duration.ofDays(7); // 7 jours pour géocodage

    /**
     * Récupère une valeur du cache.
     * 
     * @param key  Clé de cache
     * @param type Classe du type attendu
     * @return Mono avec la valeur ou Mono.empty() si absent
     */
    public <T> Mono<T> get(String key, Class<T> type) {
        return redisTemplate.opsForValue()
                .get(key)
                .cast(type)
                .doOnNext(value -> log.debug("Cache HIT: {}", key))
                .doOnError(error -> log.error("Cache GET error for key {}: {}", key, error.getMessage()))
                .onErrorResume(error -> Mono.empty());
    }

    /**
     * Met une valeur en cache avec TTL.
     * 
     * @param key   Clé de cache
     * @param value Valeur à cacher
     * @param ttl   Durée de vie
     * @return Mono<Boolean> true si succès
     */
    public Mono<Boolean> set(String key, Object value, Duration ttl) {
        return redisTemplate.opsForValue()
                .set(key, value, ttl)
                .doOnSuccess(success -> {
                    if (success) {
                        log.debug("Cache SET: {} (TTL: {})", key, ttl);
                    } else {
                        log.warn("Cache SET failed: {}", key);
                    }
                })
                .doOnError(error -> log.error("Cache SET error for key {}: {}", key, error.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Invalide une clé de cache.
     * 
     * @param key Clé à invalider
     * @return Mono<Boolean> true si la clé existait
     */
    public Mono<Boolean> delete(String key) {
        return redisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnNext(deleted -> {
                    if (deleted) {
                        log.debug("Cache DELETE: {}", key);
                    }
                })
                .doOnError(error -> log.error("Cache DELETE error for key {}: {}", key, error.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Invalide toutes les clés correspondant à un pattern.
     * 
     * @param pattern Pattern de clés (ex: "poi:*")
     * @return Mono<Long> nombre de clés supprimées
     */
    public Mono<Long> deletePattern(String pattern) {
        return redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .reduce(0L, Long::sum)
                .doOnNext(count -> log.debug("Cache DELETE pattern {}: {} keys deleted", pattern, count))
                .doOnError(error -> log.error("Cache DELETE pattern error for {}: {}", pattern, error.getMessage()))
                .onErrorReturn(0L);
    }

    /**
     * Vérifie si une clé existe dans le cache.
     * 
     * @param key Clé à vérifier
     * @return Mono<Boolean> true si la clé existe
     */
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key)
                .doOnError(error -> log.error("Cache EXISTS error for key {}: {}", key, error.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Récupère ou calcule une valeur avec cache.
     * 
     * Pattern "cache-aside": vérifie le cache, sinon calcule et met en cache.
     * 
     * @param key           Clé de cache
     * @param type          Classe du type attendu
     * @param ttl           Durée de vie
     * @param valueSupplier Fonction pour calculer la valeur si absente du cache
     * @return Mono avec la valeur (depuis cache ou calculée)
     */
    public <T> Mono<T> getOrCompute(String key, Class<T> type, Duration ttl, Mono<T> valueSupplier) {
        return get(key, type)
                .switchIfEmpty(
                        valueSupplier
                                .flatMap(value -> set(key, value, ttl)
                                        .thenReturn(value))
                                .doOnNext(value -> log.debug("Cache MISS: {} - computed and cached", key)));
    }

    /**
     * Génère une clé de cache pour un POI.
     * 
     * @param poiId ID du POI
     * @return Clé de cache
     */
    public String poiKey(Long poiId) {
        return POI_PREFIX + poiId;
    }

    /**
     * Génère une clé de cache pour une liste de POIs.
     * 
     * @param params Paramètres de la requête (page, size, category, etc.)
     * @return Clé de cache
     */
    public String poiListKey(String... params) {
        return POI_LIST_PREFIX + String.join(":", params);
    }

    /**
     * Génère une clé de cache pour une route.
     * 
     * @param hash Hash des paramètres de route (start, end, waypoints)
     * @return Clé de cache
     */
    public String routeKey(String hash) {
        return ROUTE_PREFIX + hash;
    }

    /**
     * Génère une clé de cache pour un géocodage.
     * 
     * @param city Nom de la ville
     * @return Clé de cache
     */
    public String geocodeKey(String city) {
        return GEOCODE_PREFIX + city.toLowerCase().replaceAll("\\s+", "_");
    }
}
