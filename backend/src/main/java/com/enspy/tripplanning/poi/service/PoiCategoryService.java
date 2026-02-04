package com.enspy.tripplanning.poi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.enspy.tripplanning.poi.dto.PoiCategoriesResponse;
import com.enspy.tripplanning.poi.dto.PoiCategoryDTO;
import com.enspy.tripplanning.poi.entity.PoiCategory;
import com.enspy.tripplanning.poi.repository.PoiCategoryRepository;

import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiCategoryService {

        private final PoiCategoryRepository categoryRepository;

        public Mono<PoiCategoriesResponse> getAllCategories() {
                log.debug("Récupération de toutes les catégories de POI");

                return categoryRepository.findAllByOrderByNameAsc()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList())
                                .map(categories -> PoiCategoriesResponse.builder()
                                                .categories(categories)
                                                .build())
                                .doOnSuccess(
                                                response -> log.info("Récupération réussie de {} catégories",
                                                                response.getCategories().size()))
                                .doOnError(error -> log.error("Erreur lors de la récupération des catégories", error));
        }

        public Mono<PoiCategoryDTO> getCategoryById(Long categoryId) {
                log.debug("Récupération de la catégorie avec ID: {}", categoryId);

                return categoryRepository.findById(categoryId)
                                .map(this::convertToDTO)
                                .doOnSuccess(category -> log.info("Catégorie trouvée: {}", category.getName()))
                                .doOnError(error -> log.error("Erreur lors de la récupération de la catégorie {}",
                                                categoryId, error));
        }

        private PoiCategoryDTO convertToDTO(PoiCategory category) {
                return PoiCategoryDTO.builder()
                                .categoryId(category.getCategoryId())
                                .name(category.getName())
                                .description(category.getDescription())
                                .icon(category.getIcon())
                                .color(category.getColor())
                                .build();
        }
}