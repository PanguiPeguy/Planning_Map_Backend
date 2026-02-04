package com.enspy.tripplanning.itinerary.controller;

import com.enspy.tripplanning.itinerary.entity.Itinerary;
import com.enspy.tripplanning.itinerary.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/itineraries")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    // TODO: Adaptez selon comment vous récupérez le User ID depuis le token
    // JWT/Authentication
    // Ici j'assume qu'on peut extraire l'ID, sinon on passera par un UserService
    // pour findByUsername

    @GetMapping
    public Flux<Itinerary> getMyItineraries(@RequestParam UUID userId) {
        // En prod, extraire userId du @AuthenticationPrincipal pour sécurité
        return itineraryService.getUserItineraries(userId);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Itinerary>> getItinerary(@PathVariable UUID id) {
        return itineraryService.getItinerary(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<Itinerary> createItinerary(@RequestBody Itinerary itinerary) {
        return itineraryService.saveItinerary(itinerary);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Itinerary>> updateItinerary(@PathVariable UUID id, @RequestBody Itinerary itinerary) {
        return itineraryService.getItinerary(id)
                .flatMap(existing -> {
                    itinerary.setId(id);
                    // Preserve ownership and creation date if needed, or handle in service
                    itinerary.setUserId(existing.getUserId());
                    itinerary.setCreatedAt(existing.getCreatedAt());
                    return itineraryService.saveItinerary(itinerary);
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteItinerary(@PathVariable UUID id) {
        return itineraryService.deleteItinerary(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
