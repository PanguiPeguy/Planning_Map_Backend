package com.enspy.tripplanning.statistics.controller;

import com.enspy.tripplanning.statistics.dto.DashboardStatsDTO;
import com.enspy.tripplanning.statistics.service.StatisticsService;
import com.enspy.tripplanning.authentification.entity.User;
import com.enspy.tripplanning.authentification.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

// import removed

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistics", description = "API pour les statistiques et dashboard")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtenir statistiques dashboard global")
    public Mono<DashboardStatsDTO> getDashboardStats() {
        log.info("GET /api/v1/statistics/dashboard");
        return statisticsService.getDashboardStats();
    }

    @GetMapping(value = "/user/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtenir statistiques utilisateur connecté")
    public Mono<DashboardStatsDTO> getCurrentUserStats(Authentication authentication) {
        if (authentication == null) {
            return Mono.error(new RuntimeException("Non authentifié"));
        }

        String userId;
        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            userId = ((User) principal).getUserId().toString();
        } else {
            // Fallback: try to extract from name if it looks like a UUID
            String name = authentication.getName();
            if (name != null && name.contains("-")) { // Simple UUID check
                userId = name;
            } else {
                // Try from token
                String token = authentication.getCredentials() != null ? authentication.getCredentials().toString()
                        : null;
                if (token != null && jwtTokenProvider.validateToken(token)) {
                    userId = jwtTokenProvider.getUserIdFromToken(token).toString();
                } else {
                    log.error("Impossible d'extraire l'ID utilisateur. Principal type: {}, Name: {}",
                            principal.getClass().getName(), name);
                    return Mono.error(new RuntimeException("Impossible d'extraire l'ID utilisateur"));
                }
            }
        }

        log.info("GET /api/v1/statistics/user/me - userId: {}", userId);
        return statisticsService.getUserStats(userId);
    }

    @GetMapping(value = "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtenir statistiques d'un utilisateur spécifique")
    public Mono<DashboardStatsDTO> getUserStats(@PathVariable String userId) {
        log.info("GET /api/v1/statistics/user/{}", userId);
        return statisticsService.getUserStats(userId);
    }
}
