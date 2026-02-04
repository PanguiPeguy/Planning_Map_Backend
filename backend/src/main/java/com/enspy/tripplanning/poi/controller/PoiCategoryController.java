package com.enspy.tripplanning.poi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.enspy.tripplanning.poi.dto.PoiCategoriesResponse;
import com.enspy.tripplanning.poi.service.PoiCategoryService;

import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/poi-categories")
@RequiredArgsConstructor
@Tag(name = "Catégories de POI", description = "API pour gérer les catégories de Points d'Intérêt")
public class PoiCategoryController {

        private final PoiCategoryService categoryService;

        @Operation(summary = "Récupérer toutes les catégories de POI", description = "Récupère la liste complète de toutes les catégories de Points d'Intérêt disponibles")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Liste des catégories récupérée avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PoiCategoriesResponse.class)))
        })
        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public Mono<PoiCategoriesResponse> getAllCategories() {
                log.info("GET /api/v1/poi-categories");
                return categoryService.getAllCategories();
        }
}
