package com.enspy.tripplanning.poi.service;

import com.enspy.tripplanning.notification.service.NotificationService;
import com.enspy.tripplanning.poi.dto.*;
import com.enspy.tripplanning.poi.exception.ResourceNotFoundException;
import com.enspy.tripplanning.poi.entity.Poi;
import com.enspy.tripplanning.poi.entity.PoiCategory;
import com.enspy.tripplanning.poi.repository.PoiCategoryRepository;
import com.enspy.tripplanning.poi.repository.PoiRepository;
import com.enspy.tripplanning.poi.repository.PoiFavoriteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {

    private final PoiRepository poiRepository;
    private final PoiCategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final PoiInteractionService poiInteractionService;
    private final PoiFavoriteRepository poiFavoriteRepository;

    /**
     * Récupérer tous les POI avec pagination et filtres
     */
    public Mono<PoiPageResponse> getAllPois(
            Integer page,
            Integer size,
            String category,
            Double lat,
            Double lon,
            Double radius,
            String search,
            UUID userId) {

        log.debug("Récupération des POI - page: {}, size: {}, category: {}, search: {}",
                page, size, category, search);

        // Enforce maximum size to prevent performance issues
        int effectiveSize = size != null ? Math.min(size, 50) : 20;
        Pageable pageable = PageRequest.of(page, effectiveSize);

        // Recherche par proximité
        if (lat != null && lon != null) {
            return findByProximity(lat, lon, radius != null ? radius : 10.0, page, size, userId);
        }

        // Recherche textuelle
        if (search != null && !search.isBlank()) {
            return findBySearch(search, pageable, userId);
        }

        // Filtrage par catégorie
        if (category != null && !category.isBlank()) {
            return findByCategory(category, pageable, userId);
        }

        // Todos os POI
        return findAll(pageable, userId);
    }

    public Mono<PoiPageResponse> getUserFavorites(UUID userId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);

        Flux<PoiDTO> poisFlux = poiInteractionService.getFavoritePoisByUserId(userId, pageable)
                .flatMap(this::enrichPoiWithCategory)
                .map(this::convertToDTO)
                .flatMap(dto -> enrichWithInteractionFlags(dto, userId));

        Mono<Long> countMono = poiInteractionService.countFavoriteByUserId(userId);

        return poisFlux.collectList()
                .zipWith(countMono)
                .map(tuple -> buildPageResponse(tuple.getT1(), tuple.getT2(), pageable));
    }

    private Mono<PoiDTO> enrichWithInteractionFlags(PoiDTO dto, UUID userId) {
        if (userId == null)
            return Mono.just(dto);
        return Mono.zip(
                poiInteractionService.isLiked(dto.getPoiId(), userId),
                poiInteractionService.isFavorite(dto.getPoiId(), userId)).map(tuple -> {
                    dto.setLiked(tuple.getT1()); // Corresponds to isLiked field
                    dto.setFavorite(tuple.getT2()); // Corresponds to isFavorite field
                    return dto;
                });
    }

    /**
     * Récupérer un POI par son ID
     */
    public Mono<PoiDTO> getPoiById(Long poiId, UUID userId) {
        log.debug("Récupération du POI avec ID: {}", poiId);

        return poiRepository.findById(poiId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("POI non trouvé avec l'ID: " + poiId)))
                .flatMap(this::enrichPoiWithCategory)
                .map(this::convertToDTO)
                .flatMap(dto -> enrichWithInteractionFlags(dto, userId))
                .doOnSuccess(poi -> log.info("POI trouvé: {}", poi.getName()));
    }

    /**
     * Créer un nouveau POI
     */
    public Mono<PoiDTO> createPoi(CreatePoiRequest request) {
        log.debug("Création d'un nouveau POI: {}", request.getName());

        return categoryRepository.findById(request.getCategoryId())
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Catégorie non trouvée avec l'ID: " + request.getCategoryId())))
                .flatMap(category -> {
                    Poi poi = Poi.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .latitude(BigDecimal.valueOf(request.getLatitude()))
                            .longitude(BigDecimal.valueOf(request.getLongitude()))
                            .categoryId(request.getCategoryId())
                            .addressStreet(request.getAddressStreet() != null ? request.getAddressStreet()
                                    : request.getAddress())
                            .addressCity(request.getAddressCity())
                            .addressPostalCode(request.getAddressPostalCode())
                            .addressRegion(request.getAddressRegion())
                            .addressNeighborhood(request.getAddressNeighborhood())
                            .phone(request.getPhone())
                            .rating(BigDecimal.valueOf(0.0))
                            .openingHoursJson(serializeToJson(request.getOpeningHours()))
                            .servicesJson(serializeToJson(request.getServices()))
                            .priceRange(request.getPriceRange())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return poiRepository.save(poi);
                })

                .flatMap(this::enrichPoiWithCategory)
                .map(this::convertToDTO)
                .doOnSuccess(poi -> {
                    log.info("POI créé avec succès: {} (ID: {})", poi.getName(), poi.getPoiId());
                    // Envoyer notification à tous les utilisateurs
                    notificationService.sendNewPoiNotificationToAllUsers(poi.getPoiId(), poi.getName())
                            .subscribe(
                                    notif -> log.debug("Notification envoyée: {}", notif.getNotificationId()),
                                    error -> log.error("Erreur envoi notification: {}", error.getMessage()));
                });
    }

    public Mono<PoiDTO> createPoiWithImage(String name, String categoryIdStr, String latitudeStr, String longitudeStr,
            String description, String priceLevelStr, String tagsStr, String amenitiesStr,
            String addressJson, String contactJson, String openingHoursJson,
            org.springframework.http.codec.multipart.FilePart image) {

        long categoryId = Long.parseLong(categoryIdStr);
        BigDecimal latitude = new BigDecimal(latitudeStr);
        BigDecimal longitude = new BigDecimal(longitudeStr);
        Integer priceLevel = priceLevelStr != null ? Integer.parseInt(priceLevelStr) : null;

        // Parse complex JSONs
        Map<String, String> addressMap = parseJsonMap(addressJson);
        Map<String, String> contactMap = parseJsonMap(contactJson);
        Map<String, String> openingHoursMap = parseJsonMap(openingHoursJson);

        // Parse Lists
        List<String> tagsList = parseJsonListOrComma(tagsStr);
        List<String> amenitiesList = parseJsonList(amenitiesStr);

        return categoryRepository.findById(categoryId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Catégorie non trouvée: " + categoryId)))
                .flatMap(category -> {
                    Poi.PoiBuilder builder = Poi.builder()
                            .name(name)
                            .description(description)
                            .latitude(latitude)
                            .longitude(longitude)
                            .categoryId(categoryId)
                            .priceLevel(priceLevel)
                            .rating(BigDecimal.ZERO)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now());

                    // Map Address
                    if (addressMap != null) {
                        builder.addressStreet(addressMap.get("street"));
                        builder.addressCity(addressMap.get("city"));
                        builder.addressPostalCode(addressMap.get("postal_code"));
                        builder.addressRegion(addressMap.get("region"));
                        builder.addressNeighborhood(addressMap.get("neighborhood"));
                    }

                    // Map Contact
                    if (contactMap != null) {
                        builder.phone(contactMap.get("phone"));
                        builder.email(contactMap.get("email"));
                        builder.website(contactMap.get("website"));
                    }

                    // JSON Fields
                    Poi poi = builder.build();
                    poi.setOpeningHours(openingHoursMap);
                    poi.setTags(tagsList);
                    poi.setAmenities(amenitiesList);

                    // Serialize them
                    poi.serializeAllJsonFields();

                    // Handle Image Upload
                    if (image != null) {
                        return saveImage(image).flatMap(url -> {
                            poi.setImageUrl(url);
                            return poiRepository.save(poi);
                        });
                    } else {
                        return poiRepository.save(poi);
                    }
                })
                .flatMap(this::enrichPoiWithCategory)
                .map(this::convertToDTO);
    }

    private Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isBlank())
            return new HashMap<>();

        // Vérifier si c'est un JSON valide
        json = json.trim();
        if ((json.startsWith("{") && json.endsWith("}")) ||
                (json.startsWith("[") && json.endsWith("]"))) {
            try {
                return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                log.warn("Failed to parse JSON map: {}", json);
                // Si c'est une adresse texte, la mettre dans le champ street
                Map<String, String> map = new HashMap<>();
                map.put("street", json);
                return map;
            }
        } else {
            // Si ce n'est pas un JSON, traiter comme une adresse texte
            Map<String, String> map = new HashMap<>();
            map.put("street", json);
            return map;
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank())
            return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse JSON list: {}", json);
            return new ArrayList<>();
        }
    }

    private List<String> parseJsonListOrComma(String input) {
        if (input == null || input.isBlank())
            return new ArrayList<>();
        if (input.trim().startsWith("[")) {
            return parseJsonList(input);
        } else {
            return List.of(input.split(","));
        }
    }

    private Mono<String> saveImage(org.springframework.http.codec.multipart.FilePart filePart) {
        String uploadsDir = "uploads/pois";
        java.io.File dir = new java.io.File(uploadsDir);
        if (!dir.exists())
            dir.mkdirs();

        String filename = System.currentTimeMillis() + "_" + filePart.filename().replaceAll("[^a-zA-Z0-9.-]", "_");
        java.nio.file.Path path = java.nio.file.Paths.get(uploadsDir, filename).toAbsolutePath();

        return filePart.transferTo(path)
                .then(Mono.just("/uploads/pois/" + filename));
    }

    /**
     * Mettre à jour un POI
     */
    public Mono<PoiDTO> updatePoi(Long poiId, CreatePoiRequest request) {
        log.debug("Mise à jour du POI avec ID: {}", poiId);

        return poiRepository.findById(poiId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("POI non trouvé avec l'ID: " + poiId)))
                .flatMap(existingPoi -> categoryRepository.findById(request.getCategoryId())
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                "Catégorie non trouvée avec l'ID: " + request.getCategoryId())))
                        .map(category -> {
                            existingPoi.setName(request.getName());
                            existingPoi.setDescription(request.getDescription());
                            existingPoi.setLatitude(BigDecimal.valueOf(request.getLatitude()));
                            existingPoi.setLongitude(BigDecimal.valueOf(request.getLongitude()));
                            existingPoi.setCategoryId(request.getCategoryId());
                            existingPoi.setAddressStreet(request.getAddressStreet() != null ? request.getAddressStreet()
                                    : request.getAddress());
                            existingPoi.setAddressCity(request.getAddressCity());
                            existingPoi.setAddressPostalCode(request.getAddressPostalCode());
                            existingPoi.setAddressRegion(request.getAddressRegion());
                            existingPoi.setAddressNeighborhood(request.getAddressNeighborhood());
                            existingPoi.setPhone(request.getPhone());
                            existingPoi.setOpeningHoursJson(serializeToJson(request.getOpeningHours()));
                            existingPoi.setServicesJson(serializeToJson(request.getServices()));
                            existingPoi.setPriceRange(request.getPriceRange());
                            existingPoi.setUpdatedAt(LocalDateTime.now());
                            return existingPoi;
                        }))
                .flatMap(poiRepository::save)
                .flatMap(this::enrichPoiWithCategory)
                .map(this::convertToDTO)
                .doOnSuccess(poi -> {
                    log.info("POI mis à jour: {}", poi.getName());
                    // Envoyer notification à tous les utilisateurs
                    notificationService.sendPoiEditedNotificationToUsers(poi.getPoiId(), poi.getName())
                            .subscribe(
                                    notif -> log.debug("Notification édition envoyée: {}", notif.getNotificationId()),
                                    error -> log.error("Erreur envoi notification édition: {}", error.getMessage()));
                });
    }

    /**
     * Supprimer un POI
     */
    public Mono<Void> deletePoi(Long poiId) {
        log.debug("Suppression du POI avec ID: {}", poiId);

        return poiRepository.findById(poiId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("POI non trouvé avec l'ID: " + poiId)))
                .flatMap(poi -> {
                    String poiName = poi.getName();
                    return poiRepository.delete(poi)
                            .doOnSuccess(v -> {
                                log.info("POI supprimé avec succès: {}", poiId);
                                // Notifier tous les utilisateurs de la suppression
                                notificationService.sendPoiDeletedNotificationToAllUsers(poiId, poiName)
                                        .subscribe(
                                                notif -> log.debug("Notification suppression envoyée: {}",
                                                        notif.getNotificationId()),
                                                error -> log.error(
                                                        "Erreur envoi notification suppression: {}",
                                                        error.getMessage()));
                            });
                });
    }

    /**
     * Récupérer les POI dans une zone (bounding box)
     */
    public Mono<PoiAreaResponse> getPoisInArea(
            Double minLat,
            Double minLon,
            Double maxLat,
            Double maxLon,
            List<Long> categoryIds) {

        log.debug("Récupération des POI dans la zone: [{}, {}] - [{}, {}]", minLat, minLon, maxLat, maxLon);

        Flux<Poi> poisFlux;

        if (categoryIds != null && !categoryIds.isEmpty()) {
            poisFlux = poiRepository.findInBoundingBoxByCategories(
                    minLat, minLon, maxLat, maxLon,
                    categoryIds.toArray(new Long[0]));
        } else {
            poisFlux = poiRepository.findInBoundingBox(minLat, minLon, maxLat, maxLon);
        }

        return poisFlux
                .flatMap(this::enrichPoiWithCategory)
                .map(this::convertToDTO)
                .collectList()
                .map(pois -> PoiAreaResponse.builder().pois(pois).build())
                .doOnSuccess(response -> log.info("Trouvé {} POI dans la zone", response.getPois().size()));
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private Mono<PoiPageResponse> findAll(Pageable pageable, UUID userId) {
        Flux<PoiDTO> poisFlux = poiRepository.findAllBy(pageable)
                .flatMap(this::enrichPoiWithCategory)
                .map(this::convertToDTO)
                .flatMap(dto -> enrichWithInteractionFlags(dto, userId));

        Mono<Long> countMono = poiRepository.count();

        return poisFlux.collectList()
                .zipWith(countMono)
                .map(tuple -> buildPageResponse(tuple.getT1(), tuple.getT2(), pageable));
    }

    private Mono<PoiPageResponse> findByCategory(String categoryName, Pageable pageable, UUID userId) {
        return categoryRepository.findByName(categoryName)
                .flatMap(category -> {
                    Flux<PoiDTO> poisFlux = poiRepository.findByCategoryId(category.getCategoryId(), pageable)
                            .flatMap(this::enrichPoiWithCategory)
                            .map(this::convertToDTO)
                            .flatMap(dto -> enrichWithInteractionFlags(dto, userId));

                    Mono<Long> countMono = poiRepository.countByCategoryId(category.getCategoryId());

                    return poisFlux.collectList()
                            .zipWith(countMono)
                            .map(tuple -> buildPageResponse(tuple.getT1(), tuple.getT2(), pageable));
                });
    }

    private Mono<PoiPageResponse> findBySearch(String search, Pageable pageable, UUID userId) {
        Flux<PoiDTO> poisFlux = poiRepository.searchByNameOrDescription(search, pageable)
                .flatMap(this::enrichPoiWithCategory)
                .map(this::convertToDTO)
                .flatMap(dto -> enrichWithInteractionFlags(dto, userId));

        Mono<Long> countMono = poiRepository.countBySearch(search);

        return poisFlux.collectList()
                .zipWith(countMono)
                .map(tuple -> buildPageResponse(tuple.getT1(), tuple.getT2(), pageable));
    }

    private Mono<PoiPageResponse> findByProximity(Double lat, Double lon, Double radius, Integer page, Integer size,
            UUID userId) {
        Flux<PoiDTO> poisFlux = poiRepository.findByProximity(lat, lon, radius, size, page * size)
                .flatMap(this::enrichPoiWithCategory)
                .map(this::convertToDTO)
                .flatMap(dto -> enrichWithInteractionFlags(dto, userId));

        Mono<Long> countMono = poiRepository.countByProximity(lat, lon, radius);

        return poisFlux.collectList()
                .zipWith(countMono)
                .map(tuple -> buildPageResponse(tuple.getT1(), tuple.getT2(), PageRequest.of(page, size)));
    }

    private Mono<Poi> enrichPoiWithCategory(Poi poi) {
        return categoryRepository.findById(poi.getCategoryId())
                .map(category -> {
                    poi.setCategory(category);
                    return poi;
                })
                .defaultIfEmpty(poi);
    }

    private PoiDTO convertToDTO(Poi poi) {
        PoiCategoryDTO categoryDTO = null;
        if (poi.getCategory() != null) {
            PoiCategory category = poi.getCategory();
            categoryDTO = PoiCategoryDTO.builder()
                    .categoryId(category.getCategoryId())
                    .name(category.getName())
                    .description(category.getDescription())
                    .icon(category.getIcon())
                    .color(category.getColor())
                    .build();
        }

        return PoiDTO.builder()
                .poiId(poi.getPoiId())
                .name(poi.getName())
                .description(poi.getDescription())
                .latitude(poi.getLatitude() != null ? poi.getLatitude().doubleValue() : null)
                .longitude(poi.getLongitude() != null ? poi.getLongitude().doubleValue() : null)
                .category(categoryDTO)
                .address(poi.getAddressStreet() != null
                        ? poi.getAddressStreet() + (poi.getAddressCity() != null ? ", " + poi.getAddressCity() : "")
                        : poi.getAddressCity())
                .addressStreet(poi.getAddressStreet())
                .addressCity(poi.getAddressCity())
                .addressPostalCode(poi.getAddressPostalCode())
                .addressRegion(poi.getAddressRegion())
                .addressNeighborhood(poi.getAddressNeighborhood())
                .addressCountry(poi.getAddressCountry())
                .phone(poi.getPhone())
                .rating(poi.getRating() != null ? poi.getRating().doubleValue() : null)
                .reviewCount(poi.getReviewCount())
                .likeCount(poi.getLikeCount())
                .favoriteCount(poi.getFavoriteCount())
                .openingHours(deserializeFromJson(poi.getOpeningHoursJson(), new TypeReference<Map<String, String>>() {
                }))
                .services(deserializeFromJson(poi.getServicesJson(), new TypeReference<List<String>>() {
                }))
                .priceRange(poi.getPriceRange())
                .imageUrl(poi.getImageUrl())
                .metadata(deserializeFromJson(poi.getMetadataJson(), new TypeReference<Map<String, Object>>() {
                }))
                .build();
    }

    private PoiPageResponse buildPageResponse(List<PoiDTO> content, Long totalElements, Pageable pageable) {
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        return PoiPageResponse.builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private Json serializeToJson(Object object) {
        if (object == null)
            return null;
        try {
            return Json.of(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la sérialisation JSON", e);
            return null;
        }
    }

    private <T> T deserializeFromJson(Json json, TypeReference<T> typeReference) {
        if (json == null)
            return null;
        String jsonString = json.asString();
        if (jsonString == null || jsonString.isBlank())
            return null;
        try {
            return objectMapper.readValue(jsonString, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de la désérialisation JSON", e);
            return null;
        }
    }
}