package com.enspy.tripplanning.poi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.enspy.tripplanning.poi.dto.*;
import com.enspy.tripplanning.poi.service.PoiService;
import com.enspy.tripplanning.poi.service.PoiInteractionService;
import com.enspy.tripplanning.authentification.security.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/api/v1/pois")
@RequiredArgsConstructor
@Tag(name = "Points d'Intérêt (POI)", description = "API pour gérer les Points d'Intérêt")
public class PoiController {

        private final PoiService poiService;
        private final PoiInteractionService poiInteractionService;
        private final JwtTokenProvider jwtTokenProvider;

        @Operation(summary = "Récupérer tous les POI", description = "Récupère la liste de tous les POI avec pagination et filtres optionnels (catégorie, recherche, proximité)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Liste des POI récupérée avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PoiPageResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Paramètres invalides")
        })
        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public Mono<PoiPageResponse> getAllPois(
                        @Parameter(description = "Numéro de la page (commence à 0)", example = "0") @RequestParam(defaultValue = "0") Integer page,

                        @Parameter(description = "Taille de la page", example = "20") @RequestParam(defaultValue = "20") Integer size,

                        @Parameter(description = "Nom de la catégorie pour filtrer", example = "Hébergement") @RequestParam(required = false) String category,

                        @Parameter(description = "Latitude pour recherche de proximité", example = "3.8667") @RequestParam(required = false) Double lat,

                        @Parameter(description = "Longitude pour recherche de proximité", example = "11.5167") @RequestParam(required = false) Double lon,

                        @Parameter(description = "Rayon de recherche en km", example = "10") @RequestParam(required = false) Double radius,

                        @Parameter(description = "Terme de recherche textuelle", example = "hotel") @RequestParam(required = false) String search,
                        Authentication authentication) {
                log.info("GET /api/v1/pois - page: {}, size: {}, category: {}, search: {}",
                                page, size, category, search);

                UUID userId = tryExtractUserId(authentication);
                return poiService.getAllPois(page, size, category, lat, lon, radius, search, userId);
        }

        @Operation(summary = "Récupérer un POI par son ID", description = "Récupère les détails complets d'un Point d'Intérêt spécifique")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "POI trouvé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PoiDTO.class))),
                        @ApiResponse(responseCode = "404", description = "POI non trouvé")
        })
        @GetMapping(value = "/{poiId}", produces = MediaType.APPLICATION_JSON_VALUE)
        public Mono<PoiDTO> getPoiById(
                        @Parameter(description = "ID du POI", example = "1", required = true) @PathVariable Long poiId,
                        Authentication authentication) {
                log.info("GET /api/v1/pois/{}", poiId);
                UUID userId = tryExtractUserId(authentication);
                return poiService.getPoiById(poiId, userId);
        }

        @Operation(summary = "Créer un nouveau POI", description = "Crée un nouveau Point d'Intérêt avec image optionnelle", security = @SecurityRequirement(name = "bearer-jwt"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "POI créé avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PoiDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Données invalides"),
                        @ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @ApiResponse(responseCode = "404", description = "Catégorie non trouvée")
        })
        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        @ResponseStatus(HttpStatus.CREATED)
        public Mono<PoiDTO> createPoi(
                        @RequestPart("name") String name,
                        @RequestPart("categoryId") String categoryIdStr,
                        @RequestPart("latitude") String latitudeStr,
                        @RequestPart("longitude") String longitudeStr,
                        @RequestPart(value = "description", required = false) String description,
                        @RequestPart(value = "price_level", required = false) String priceLevelStr,
                        @RequestPart(value = "tags", required = false) String tagsStr, // Comma separated or JSON list
                        @RequestPart(value = "amenities", required = false) String amenitiesStr, // JSON List
                        @RequestPart(value = "address", required = false) String addressJson,
                        @RequestPart(value = "contact", required = false) String contactJson,
                        @RequestPart(value = "opening_hours", required = false) String openingHoursJson,
                        @RequestPart(value = "image", required = false) org.springframework.http.codec.multipart.FilePart image) {

                log.info("POST /api/v1/pois [Multipart] - name: {}", name);

                // Parsing manually to CreatePoiRequest or passing to dedicated service method
                return poiService.createPoiWithImage(
                                name, categoryIdStr, latitudeStr, longitudeStr, description,
                                priceLevelStr, tagsStr, amenitiesStr, addressJson, contactJson,
                                openingHoursJson, image);
        }

        @Operation(summary = "Mettre à jour un POI", description = "Met à jour un Point d'Intérêt existant (nécessite l'authentification admin)", security = @SecurityRequirement(name = "bearer-jwt"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "POI mis à jour avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PoiDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Données invalides"),
                        @ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @ApiResponse(responseCode = "404", description = "POI ou catégorie non trouvé")
        })
        @PutMapping(value = "/{poiId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public Mono<PoiDTO> updatePoi(
                        @Parameter(description = "ID du POI à mettre à jour", example = "1", required = true) @PathVariable Long poiId,

                        @Parameter(description = "Nouvelles données du POI", required = true) @Valid @RequestBody CreatePoiRequest request) {
                log.info("PUT /api/v1/pois/{} - name: {}", poiId, request.getName());
                return poiService.updatePoi(poiId, request);
        }

        @Operation(summary = "Supprimer un POI", description = "Supprime un Point d'Intérêt (nécessite l'authentification admin)", security = @SecurityRequirement(name = "bearer-jwt"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "POI supprimé avec succès"),
                        @ApiResponse(responseCode = "401", description = "Non authentifié"),
                        @ApiResponse(responseCode = "404", description = "POI non trouvé")
        })
        @DeleteMapping("/{poiId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public Mono<Void> deletePoi(
                        @Parameter(description = "ID du POI à supprimer", example = "1", required = true) @PathVariable Long poiId) {
                log.info("DELETE /api/v1/pois/{}", poiId);
                return poiService.deletePoi(poiId);
        }

        @Operation(summary = "Récupérer les POI dans une zone", description = "Récupère tous les POI situés dans une zone géographique définie par un bounding box")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "POI dans la zone récupérés avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PoiAreaResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Paramètres invalides")
        })
        @GetMapping(value = "/area", produces = MediaType.APPLICATION_JSON_VALUE)
        public Mono<PoiAreaResponse> getPoisInArea(
                        @Parameter(description = "Latitude minimale", example = "3.8", required = true) @RequestParam Double minLat,

                        @Parameter(description = "Longitude minimale", example = "11.4", required = true) @RequestParam Double minLon,

                        @Parameter(description = "Latitude maximale", example = "3.9", required = true) @RequestParam Double maxLat,

                        @Parameter(description = "Longitude maximale", example = "11.6", required = true) @RequestParam Double maxLon,

                        @Parameter(description = "Liste des IDs de catégories pour filtrer", example = "1,2,3") @RequestParam(required = false) List<Long> categories) {
                log.info("GET /api/v1/pois/area - minLat: {}, minLon: {}, maxLat: {}, maxLon: {}",
                                minLat, minLon, maxLat, maxLon);

                return poiService.getPoisInArea(minLat, minLon, maxLat, maxLon, categories);
        }

        // ============================================================
        // ENDPOINTS D'INTERACTION (Likes & Favoris)
        // ============================================================

        @Operation(summary = "Liker un POI", security = @SecurityRequirement(name = "bearer-jwt"))
        @PostMapping("/{poiId}/like")
        public Mono<Void> likePoi(@PathVariable Long poiId, Authentication authentication) {
                return poiInteractionService.likePoi(poiId, getUserId(authentication));
        }

        @Operation(summary = "Enlever un like d'un POI", security = @SecurityRequirement(name = "bearer-jwt"))
        @DeleteMapping("/{poiId}/like")
        public Mono<Void> unlikePoi(@PathVariable Long poiId, Authentication authentication) {
                return poiInteractionService.unlikePoi(poiId, getUserId(authentication));
        }

        @Operation(summary = "Ajouter un POI aux favoris", security = @SecurityRequirement(name = "bearer-jwt"))
        @PostMapping("/{poiId}/favorite")
        public Mono<Void> addFavorite(
                        @PathVariable Long poiId,
                        @RequestParam(required = false) String notes,
                        Authentication authentication) {
                return poiInteractionService.addFavorite(poiId, getUserId(authentication), notes);
        }

        @Operation(summary = "Retirer un POI des favoris", security = @SecurityRequirement(name = "bearer-jwt"))
        @DeleteMapping("/{poiId}/favorite")
        public Mono<Void> removeFavorite(@PathVariable Long poiId, Authentication authentication) {
                return poiInteractionService.removeFavorite(poiId, getUserId(authentication));
        }

        @Operation(summary = "Récupérer les favoris de l'utilisateur connecté", security = @SecurityRequirement(name = "bearer-jwt"))
        @GetMapping("/favorites")
        public Mono<PoiPageResponse> getUserFavorites(
                        @Parameter(description = "Numéro de la page (commence à 0)", example = "0") @RequestParam(defaultValue = "0") Integer page,
                        @Parameter(description = "Taille de la page", example = "20") @RequestParam(defaultValue = "20") Integer size,
                        Authentication authentication) {
                return poiService.getUserFavorites(getUserId(authentication), page, size);
        }

        // ============================================================
        // ENDPOINTS DE STATISTIQUES (Compteurs)
        // ============================================================

        @Operation(summary = "Récupérer le nombre de likes d'un POI")
        @GetMapping("/{poiId}/likes/count")
        public Mono<Long> getPoiLikeCount(@PathVariable Long poiId) {
                return poiInteractionService.getLikeCount(poiId);
        }

        @Operation(summary = "Récupérer le nombre de favoris d'un POI")
        @GetMapping("/{poiId}/favorites/count")
        public Mono<Long> getPoiFavoriteCount(@PathVariable Long poiId) {
                return poiInteractionService.getFavoriteCount(poiId);
        }

        // ============================================================
        // MÉTHODES UTILITAIRES
        // ============================================================

        private UUID getUserId(Authentication authentication) {
                if (authentication == null)
                        throw new RuntimeException("Utilisateur non authentifié");
                UUID userId = tryExtractUserId(authentication);
                if (userId == null)
                        throw new RuntimeException("ID utilisateur non valide");
                return userId;
        }

        private UUID tryExtractUserId(Authentication authentication) {
                try {
                        if (authentication != null && authentication.getPrincipal() != null) {
                                Object principal = authentication.getPrincipal();

                                // Cas standard: User complet (UserDetails)
                                if (principal instanceof com.enspy.tripplanning.authentification.entity.User) {
                                        return ((com.enspy.tripplanning.authentification.entity.User) principal)
                                                        .getUserId();
                                }

                                // Cas où seul le token est présent (échec auth mais principal = token string)
                                if (principal instanceof String && !((String) principal).isBlank()) {
                                        String token = (String) principal;
                                        if (jwtTokenProvider.validateToken(token)) {
                                                return jwtTokenProvider.getUserIdFromToken(token);
                                        }
                                }

                                // Cas via name (fallback)
                                if (authentication.getName() != null) {
                                        return UUID.fromString(authentication.getName());
                                }
                        }
                } catch (Exception e) {
                        log.warn("Impossible d'extraire le userId de l'authentification: {}", e.getMessage());
                }
                return null;
        }
}