package com.enspy.tripplanning.trip.controller;

import com.enspy.tripplanning.trip.dto.*;
import com.enspy.tripplanning.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * ================================================================
 * TripController - API REST Gestion Voyages
 * ================================================================
 * 
 * 🎯 ENDPOINTS:
 * - POST   /api/v1/trips              → Créer voyage
 * - GET    /api/v1/trips              → Lister mes voyages
 * - GET    /api/v1/trips/{id}         → Détails voyage
 * - PUT    /api/v1/trips/{id}         → Modifier voyage
 * - DELETE /api/v1/trips/{id}         → Supprimer voyage
 * - GET    /api/v1/trips/share/{token} → Accès via lien
 * 
 * 🔐 SÉCURITÉ:
 * - Tous endpoints protégés JWT (sauf /share/{token})
 * - userId extrait depuis Authentication (SecurityContext)
 * 
 * 📚 CONCEPTS PÉDAGOGIQUES:
 * 
 * 1️⃣ REST API DESIGN:
 * - POST = création
 * - GET = lecture
 * - PUT = modification complète
 * - DELETE = suppression
 * - Codes HTTP: 200 OK, 201 Created, 204 No Content, 404 Not Found
 * 
 * 2️⃣ SPRING SECURITY INTEGRATION:
 * - Authentication = contexte sécurité Spring
 * - Contient UserDetails chargé par JwtFilter
 * - getPrincipal() = User entity
 * 
 * 3️⃣ VALIDATION:
 * - @Valid = déclenche validation Bean Validation (JSR-380)
 * - @NotBlank, @Size, @Email dans DTOs
 * - Erreurs 400 Bad Request automatiques
 * 
 * ================================================================
 * @author Thomas Djotio Ndié
 * @since 2024-12-18
 * ================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Gestion des voyages planifiés")
public class TripController {

    private final TripService tripService;

    // ============================================================
    // CREATE
    // ============================================================
    
    @Operation(
        summary = "Créer un nouveau voyage",
        description = "Crée un voyage et ajoute automatiquement le créateur comme OWNER",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TripResponse> createTrip(
        @Valid @RequestBody CreateTripRequest request,
        Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        log.info("POST /api/v1/trips - user: {}, titre: '{}'", userId, request.getTitle());
        
        return tripService.createTrip(request, userId);
    }

    // ============================================================
    // READ
    // ============================================================
    
    @Operation(
        summary = "Lister mes voyages",
        description = "Retourne tous les voyages possédés + voyages partagés avec moi",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<TripResponse> getMyTrips(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("GET /api/v1/trips - user: {}", userId);
        
        return tripService.getUserTrips(userId);
    }

    @Operation(
        summary = "Détails d'un voyage",
        description = "Récupère un voyage avec waypoints, membres, statistiques",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping(
        value = "/{tripId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<TripResponse> getTripById(
        @Parameter(description = "ID du voyage") @PathVariable UUID tripId,
        Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        log.info("GET /api/v1/trips/{} - user: {}", tripId, userId);
        
        return tripService.getTripById(tripId, userId);
    }

    @Operation(
        summary = "Accès voyage via lien de partage",
        description = "Permet d'accéder à un voyage public via son token (sans authentification si public)"
    )
    @GetMapping(
        value = "/share/{shareToken}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<TripResponse> getTripByShareToken(
        @Parameter(description = "Token de partage unique") @PathVariable String shareToken
    ) {
        log.info("GET /api/v1/trips/share/{}", shareToken);
        
        return tripService.getTripByShareToken(shareToken);
    }

    // ============================================================
    // UPDATE
    // ============================================================
    
    @Operation(
        summary = "Modifier un voyage",
        description = "Seul OWNER ou EDITOR peuvent modifier",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PutMapping(
        value = "/{tripId}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<TripResponse> updateTrip(
        @Parameter(description = "ID du voyage") @PathVariable UUID tripId,
        @Valid @RequestBody UpdateTripRequest request,
        Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        log.info("PUT /api/v1/trips/{} - user: {}", tripId, userId);
        
        return tripService.updateTrip(tripId, request, userId);
    }

    // ============================================================
    // DELETE
    // ============================================================
    
    @Operation(
        summary = "Supprimer un voyage",
        description = "Seul OWNER peut supprimer. Supprime aussi waypoints + membres",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @DeleteMapping("/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTrip(
        @Parameter(description = "ID du voyage") @PathVariable UUID tripId,
        Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        log.info("DELETE /api/v1/trips/{} - user: {}", tripId, userId);
        
        return tripService.deleteTrip(tripId, userId);
    }

    // ============================================================
    // UTILITAIRES
    // ============================================================
    
    /**
     * Extrait UUID utilisateur depuis contexte Spring Security
     * 
     * 🔐 FLOW SÉCURITÉ:
     * 1. JwtAuthenticationFilter valide token
     * 2. Charge User depuis DB
     * 3. Inject dans SecurityContext
     * 4. Authentication.getPrincipal() = User
     * 
     * @param authentication Contexte sécurité
     * @return UUID utilisateur connecté
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        
        // Principal = User entity (UserDetails)
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof com.enspy.tripplanning.authentification.entity.User) {
            com.enspy.tripplanning.authentification.entity.User user = 
                (com.enspy.tripplanning.authentification.entity.User) principal;
            return user.getUserId();
        }
        
        throw new RuntimeException("Type principal invalide");
    }
}