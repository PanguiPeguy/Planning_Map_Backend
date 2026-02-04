package com.enspy.tripplanning.itinerary.service;

import com.enspy.tripplanning.itinerary.entity.Itinerary;
import com.enspy.tripplanning.itinerary.repository.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;

    /**
     * Get all itineraries for a specific user
     */
    public Flux<Itinerary> getUserItineraries(UUID userId) {
        return itineraryRepository.findByUserId(userId);
    }

    /**
     * Get a specific itinerary by ID
     */
    public Mono<Itinerary> getItinerary(UUID id) {
        return itineraryRepository.findById(id);
    }

    /**
     * Create or Update an Itinerary
     */
    public Mono<Itinerary> saveItinerary(Itinerary itinerary) {
        if (itinerary.getId() == null) {
            itinerary.setNew(true);
            itinerary.setCreatedAt(LocalDateTime.now());
        }
        itinerary.setUpdatedAt(LocalDateTime.now());
        return itineraryRepository.save(itinerary);
    }

    /**
     * Delete an itinerary
     */
    public Mono<Void> deleteItinerary(UUID id) {
        return itineraryRepository.deleteById(id);
    }
}
