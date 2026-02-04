package com.enspy.tripplanning.itinerary.repository;

import com.enspy.tripplanning.itinerary.entity.Itinerary;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface ItineraryRepository extends ReactiveCrudRepository<Itinerary, UUID> {
    Flux<Itinerary> findByUserId(UUID userId);
}
