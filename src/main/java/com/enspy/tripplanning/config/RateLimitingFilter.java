package com.enspy.tripplanning.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtre de limitation de débit (Rate Limiting).
 * 
 * Protection essentielle contre les attaques par déni de service (DDoS)
 * et les surcharges du moteur de calcul d'itinéraires.
 * 
 * Limite actuelle : 60 requêtes par minute par adresse IP pour le routing.
 */
@Slf4j
@Component
public class RateLimitingFilter implements WebFilter {

    private final Map<String, UserRateLimit> userLimits = new ConcurrentHashMap<>();

    // Configuration : 60 requêtes par minute
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Appliquer la restriction uniquement sur les endpoints de calcul d'itinéraires
        if (!path.contains("/api/v1/routing")) {
            return chain.filter(exchange);
        }

        String ip = getClientIp(exchange);
        long currentMinute = System.currentTimeMillis() / 60000;

        UserRateLimit limit = userLimits.computeIfAbsent(ip, k -> new UserRateLimit(currentMinute));

        // Vérification et mise à jour atomique du compteur
        int requestCount;
        synchronized (limit) {
            if (limit.minute != currentMinute) {
                limit.minute = currentMinute;
                limit.count.set(0);
            }
            requestCount = limit.count.incrementAndGet();
        }

        if (requestCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("⚠️ Rate limit dépassé pour l'IP : {} sur le chemin : {}", ip, path);
            return writeErrorResponse(exchange);
        }

        return chain.filter(exchange);
    }

    /**
     * Extrait l'IP réelle du client (gère les proxys comme Cloudflare).
     */
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    /**
     * Retourne une erreur 429 (Too Many Requests) formatée en JSON.
     */
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonError = String.format(
                "{\"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Limite de %d requêtes par minute dépassée pour le calcul d'itinéraires.\"}",
                MAX_REQUESTS_PER_MINUTE);

        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(jsonError.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Structure interne pour le suivi des requêtes par IP.
     */
    private static class UserRateLimit {
        long minute;
        final AtomicInteger count = new AtomicInteger(0);

        UserRateLimit(long minute) {
            this.minute = minute;
        }
    }
}
