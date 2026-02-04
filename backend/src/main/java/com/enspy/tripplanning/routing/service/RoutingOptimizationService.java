package com.enspy.tripplanning.routing.service;

import com.enspy.tripplanning.routing.dto.*;
import com.enspy.tripplanning.routing.model.RoadEdge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service d'optimisation avancée pour le routing.
 * 
 * Implémente :
 * 1. Warm-up du cache (Pré-calcul des routes majeures)
 * 2. Logique de filtrage multi-niveaux (Highway Hierarchies simplified)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingOptimizationService implements CommandLineRunner {

    // CORRECTION: Injecter ApplicationContext au lieu de RouteCalculatorService
    // pour récupérer le service après l'initialisation complète
    private final ApplicationContext applicationContext;

    // Distances pour le filtrage multi-niveaux (en km)
    private static final double LOCAL_SEARCH_RADIUS = 5.0;

    /**
     * Liste des villes majeures du Cameroun pour le pré-calcul (City Centers).
     */
    private static final List<CityCenter> MAJOR_CITIES = List.of(
            new CityCenter("Yaoundé", 3.8480, 11.5021),
            new CityCenter("Douala", 4.0530, 9.7000),
            new CityCenter("Bafoussam", 5.4737, 10.4177),
            new CityCenter("Garoua", 9.3000, 13.4000),
            new CityCenter("Maroua", 10.5967, 14.3167),
            new CityCenter("Ngaoundéré", 7.3167, 13.5833),
            new CityCenter("Bertoua", 4.5767, 13.6783),
            new CityCenter("Bamenda", 5.9597, 10.1453),
            new CityCenter("Buea", 4.1567, 9.2324),
            new CityCenter("Kribi", 2.9400, 9.9100));

    @Override
    public void run(String... args) {
        // Lancer les pré-calculs après un court délai pour laisser le serveur démarrer
        Mono.delay(java.time.Duration.ofSeconds(30))
                .then(warmUpCache())
                .subscribe();
    }

    /**
     * Pré-calcule les routes entre les villes majeures.
     * (Disabled due to Redis removal)
     */
    public Mono<Void> warmUpCache() {
        log.info("🚀 Warm-up disabled (Redis removed).");
        return Mono.empty();
    }

    /**
     * Applique un filtrage multi-niveaux sur les arêtes.
     * Réalise le point "Multi-level Graph".
     * 
     * @param edge          L'arête à tester
     * @param distFromStart Distance du nœud source au départ
     * @param distFromEnd   Distance du nœud cible à l'arrivée
     * @return true si l'arête doit être explorée
     */
    public boolean shouldExploreEdge(RoadEdge edge, double distFromStart, double distFromEnd) {
        // Désactivé temporairement pour le débogage
        return true;
    }

    private record CityCenter(String name, double lat, double lon) {
    }
}