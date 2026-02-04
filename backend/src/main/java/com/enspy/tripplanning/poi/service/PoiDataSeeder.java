package com.enspy.tripplanning.poi.service;

import com.enspy.tripplanning.poi.entity.Poi;
import com.enspy.tripplanning.poi.repository.PoiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class PoiDataSeeder implements CommandLineRunner {

    private final PoiRepository poiRepository;

    @Override
    public void run(String... args) throws Exception {
        checkAndSeedPois();
    }

    private void checkAndSeedPois() {
        poiRepository.count()
                .flatMap(count -> {
                    if (count < 100) { // Seed if fewer than 100 POIs
                        log.info("Base de données POI vide ou presque. Lancement du seeding...");
                        return seedPois();
                    } else {
                        log.info("POIs déjà présents ({}). Seeding ignoré.", count);
                        return Mono.empty();
                    }
                })
                .subscribe();
    }

    private Mono<Void> seedPois() {
        List<Poi> pois = new ArrayList<>();

        // ========== VRAIES AGENCES DE BUS AU CAMEROUN ==========

        // YAOUNDÉ - Capitale
        pois.add(createPoi("Touristique Express", "Gare Yaoundé 1", 3.8480, 11.5021, "Yaoundé", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Yaoundé 2", 3.8650, 11.5180, "Yaoundé", "Transport"));
        pois.add(createPoi("General Express Voyages", "Gare routière Mvan", 3.8760, 11.5160, "Yaoundé", "Transport"));
        pois.add(createPoi("Buca Voyages", "Direction Sud (Ebolowa, Kribi)", 3.8600, 11.5100, "Yaoundé", "Transport"));
        pois.add(createPoi("United Express", "Agence VIP Yaoundé", 3.8520, 11.5050, "Yaoundé", "Transport"));
        pois.add(createPoi("Cerises Express VIP", "Agence Yaoundé", 3.8580, 11.5120, "Yaoundé", "Transport"));
        pois.add(createPoi("Global Voyages", "Gare Yaoundé", 3.8670, 11.5200, "Yaoundé", "Transport"));

        // DOUALA - Capitale économique
        pois.add(createPoi("Touristique Express", "Gare Douala 1 - Akwa", 4.0530, 9.7000, "Douala", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Douala 2 - Bonabéri", 4.0680, 9.6850, "Douala", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Douala 3", 4.0450, 9.7100, "Douala", "Transport"));
        pois.add(createPoi("Finexs Voyages", "Agence Douala Akwa", 4.0530, 9.7000, "Douala", "Transport"));
        pois.add(createPoi("United Express", "Agence VIP Douala", 4.0500, 9.6980, "Douala", "Transport"));
        pois.add(createPoi("Cerises Express VIP", "Gare Douala Akwa", 4.0520, 9.6990, "Douala", "Transport"));
        pois.add(createPoi("Musango Bus Services", "Gare Douala", 4.0600, 9.7050, "Douala", "Transport"));
        pois.add(createPoi("Afrique Connection", "Terminal Douala-Nigeria", 4.0580, 9.7020, "Douala", "Transport"));
        pois.add(createPoi("Men Travel", "Liaisons Douala-Kribi", 4.0600, 9.7100, "Douala", "Transport"));
        pois.add(createPoi("Global Voyages", "Gare Douala", 4.0550, 9.7030, "Douala", "Transport"));

        // BAFOUSSAM - Région de l'Ouest
        pois.add(createPoi("Trésor Voyages", "Gare Bafoussam", 5.4737, 10.4177, "Bafoussam", "Transport"));
        pois.add(createPoi("Amour Mezam Express", "Agence Bafoussam", 5.4750, 10.4200, "Bafoussam", "Transport"));
        pois.add(createPoi("Global Voyages", "Gare Bafoussam", 5.4760, 10.4190, "Bafoussam", "Transport"));
        pois.add(createPoi("Vatican Express", "Terminal Bafoussam", 5.4720, 10.4150, "Bafoussam", "Transport"));

        // BAMENDA - Région du Nord-Ouest
        pois.add(createPoi("Amour Mezam Express", "Siège Principal Bamenda", 5.9597, 10.1453, "Bamenda", "Transport"));
        pois.add(createPoi("Nso Boyz Transport", "Gare Bamenda", 5.9550, 10.1500, "Bamenda", "Transport"));
        pois.add(createPoi("Moghamo Bus", "Terminal Bamenda", 5.9600, 10.1480, "Bamenda", "Transport"));
        pois.add(createPoi("Epic Rides & Tours", "Agence Bamenda", 5.9580, 10.1460, "Bamenda", "Transport"));

        // BUEA - Ville universitaire
        pois.add(createPoi("Musango Bus Services", "Gare Buea", 4.1567, 9.2324, "Buea", "Transport"));
        pois.add(createPoi("Amour Mezam Express", "Agence Buea", 4.1580, 9.2340, "Buea", "Transport"));
        pois.add(createPoi("Afrique Connection", "Terminal Buea", 4.1560, 9.2310, "Buea", "Transport"));

        // LIMBE - Ville côtière
        pois.add(createPoi("Amour Mezam Express", "Agence Limbé", 4.0167, 9.2000, "Limbé", "Transport"));
        pois.add(createPoi("Musango Bus Services", "Gare Limbé", 4.0180, 9.2020, "Limbé", "Transport"));

        // KUMBA - Sud-Ouest
        pois.add(createPoi("Amour Mezam Express", "Gare Kumba", 4.6333, 9.4500, "Kumba", "Transport"));
        pois.add(createPoi("Musango Bus Services", "Terminal Kumba", 4.6350, 9.4520, "Kumba", "Transport"));

        // NKONGSAMBA - Littoral
        pois.add(createPoi("Touristique Express", "Gare Nkongsamba", 4.9547, 9.9383, "Nkongsamba", "Transport"));
        pois.add(createPoi("Transit Voyages", "Terminal Nkongsamba", 4.9560, 9.9400, "Nkongsamba", "Transport"));

        // EDEA - Centre hydroélectrique
        pois.add(createPoi("Central Express", "Gare Edéa", 3.7967, 10.1333, "Edéa", "Transport"));
        pois.add(createPoi("Littoral Voyages", "Terminal Edéa", 3.7980, 10.1350, "Edéa", "Transport"));

        // KRIBI - Ville balnéaire
        pois.add(createPoi("Buca Voyages", "Gare Kribi", 2.9400, 9.9100, "Kribi", "Transport"));
        pois.add(createPoi("Men Travel", "Terminal Kribi Plage", 2.9420, 9.9120, "Kribi", "Transport"));
        pois.add(createPoi("Ocean Express", "Agence Kribi", 2.9380, 9.9080, "Kribi", "Transport"));

        // EBOLOWA - Sud
        pois.add(createPoi("Buca Voyages", "Gare Ebolowa", 2.9000, 11.1500, "Ebolowa", "Transport"));
        pois.add(createPoi("Sud Express", "Terminal Ebolowa", 2.9020, 11.1520, "Ebolowa", "Transport"));

        // BERTOUA - Est
        pois.add(createPoi("Touristique Express", "Gare Bertoua", 4.5767, 13.6783, "Bertoua", "Transport"));
        pois.add(createPoi("Est Voyages", "Terminal Bertoua", 4.5780, 13.6800, "Bertoua", "Transport"));
        pois.add(createPoi("Central Express", "Agence Bertoua", 4.5750, 13.6760, "Bertoua", "Transport"));

        // NGAOUNDÉRÉ - Adamaoua
        pois.add(createPoi("Touristique Express", "Gare Ngaoundéré 1", 7.3167, 13.5833, "Ngaoundéré", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Ngaoundéré 2", 7.3200, 13.5850, "Ngaoundéré", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Ngaoundéré 3", 7.3180, 13.5820, "Ngaoundéré", "Transport"));
        pois.add(createPoi("Adamaoua Voyages", "Terminal Ngaoundéré", 7.3150, 13.5800, "Ngaoundéré", "Transport"));

        // GAROUA - Nord
        pois.add(createPoi("Touristique Express", "Gare Garoua", 9.3000, 13.4000, "Garoua", "Transport"));
        pois.add(createPoi("Grand Nord Express", "Terminal Garoua", 9.3020, 13.4020, "Garoua", "Transport"));
        pois.add(createPoi("Bénoué Voyages", "Agence Garoua", 9.2980, 13.3980, "Garoua", "Transport"));

        // MAROUA - Extrême-Nord
        pois.add(createPoi("Touristique Express", "Gare Maroua 1", 10.5967, 14.3167, "Maroua", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Maroua 2", 10.6000, 14.3200, "Maroua", "Transport"));
        pois.add(createPoi("Sahel Express", "Terminal Maroua", 10.5950, 14.3150, "Maroua", "Transport"));

        // Villes secondaires importantes
        pois.add(createPoi("Touristique Express", "Gare Belabo", 4.9333, 13.3000, "Belabo", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Meiganga", 6.5167, 14.3000, "Meiganga", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Garoua-Boulaï", 5.8833, 14.5333, "Garoua-Boulaï", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Kousséri", 12.0833, 15.0333, "Kousséri", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Touboro", 7.6667, 15.3500, "Touboro", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Yagoua", 10.3333, 15.2333, "Yagoua", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Kaélé", 10.0833, 14.4500, "Kaélé", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Abong-Mbang", 3.9833, 13.1833, "Abong-Mbang", "Transport"));
        pois.add(createPoi("Touristique Express", "Gare Guidiguis", 9.6000, 14.6667, "Guidiguis", "Transport"));
        pois.add(createPoi("Amour Mezam Express", "Gare Kumbo", 6.2033, 10.6783, "Kumbo", "Transport"));
        pois.add(createPoi("Amour Mezam Express", "Gare Fundong", 6.1967, 10.2717, "Fundong", "Transport"));
        pois.add(createPoi("Amour Mezam Express", "Gare Nkambe", 6.5833, 10.7167, "Nkambe", "Transport"));
        pois.add(createPoi("Amour Mezam Express", "Gare Ndop", 5.9667, 10.4500, "Ndop", "Transport"));
        pois.add(createPoi("Global Voyages", "Gare Dschang", 5.4500, 10.0667, "Dschang", "Transport"));
        pois.add(createPoi("Global Voyages", "Gare Mbouda", 5.6267, 10.2533, "Mbouda", "Transport"));
        pois.add(createPoi("Trésor Voyages", "Gare Foumban", 5.7267, 10.9000, "Foumban", "Transport"));

        // ========== ARRÊTS GÉNÉRÉS SUR LES CORRIDORS PRINCIPAUX ==========

        // Corridor 1: Yaoundé -> Douala (250 km, axe principal) - 300 arrêts
        pois.addAll(generateStopsAlongRoute("Route Yaoundé-Douala", 3.8480, 11.5021, 4.0530, 9.7000, 300));

        // Corridor 2: Yaoundé -> Bafoussam (300 km) - 300 arrêts
        pois.addAll(generateStopsAlongRoute("Route Yaoundé-Bafoussam", 3.8480, 11.5021, 5.4737, 10.4177, 300));

        // Corridor 3: Douala -> Bafoussam (280 km) - 200 arrêts
        pois.addAll(generateStopsAlongRoute("Route Douala-Bafoussam", 4.0530, 9.7000, 5.4737, 10.4177, 200));

        // Corridor 4: Yaoundé -> Bertoua (Est, 350 km) - 150 arrêts
        pois.addAll(generateStopsAlongRoute("Route Yaoundé-Bertoua", 3.8480, 11.5021, 4.5767, 13.6783, 150));

        // Corridor 5: Douala -> Kribi (Sud, 150 km) - 100 arrêts
        pois.addAll(generateStopsAlongRoute("Route Douala-Kribi", 4.0530, 9.7000, 2.9400, 9.9100, 100));

        // Corridor 6: Yaoundé -> Ngaoundéré (Nord, 470 km) - 250 arrêts
        pois.addAll(generateStopsAlongRoute("Route Yaoundé-Ngaoundéré", 3.8480, 11.5021, 7.3167, 13.5833, 250));

        // Corridor 7: Douala -> Limbé via Buea (côte, 80 km) - 80 arrêts
        pois.addAll(generateStopsAlongRoute("Route Douala-Buea-Limbé", 4.0530, 9.7000, 4.0167, 9.2000, 80));

        // Corridor 8: Douala -> Kumba (Sud-Ouest, 135 km) - 100 arrêts
        pois.addAll(generateStopsAlongRoute("Route Douala-Kumba", 4.0530, 9.7000, 4.6333, 9.4500, 100));

        // Corridor 9: Yaoundé -> Ebolowa (Sud, 168 km) - 120 arrêts
        pois.addAll(generateStopsAlongRoute("Route Yaoundé-Ebolowa", 3.8480, 11.5021, 2.9000, 11.1500, 120));

        // Corridor 10: Ngaoundéré -> Garoua (Nord, 285 km) - 150 arrêts
        pois.addAll(generateStopsAlongRoute("Route Ngaoundéré-Garoua", 7.3167, 13.5833, 9.3000, 13.4000, 150));

        // Corridor 11: Garoua -> Maroua (Extrême-Nord, 245 km) - 150 arrêts
        pois.addAll(generateStopsAlongRoute("Route Garoua-Maroua", 9.3000, 13.4000, 10.5967, 14.3167, 150));

        // Corridor 12: Bafoussam -> Bamenda (Nord-Ouest, 72 km) - 80 arrêts
        pois.addAll(generateStopsAlongRoute("Route Bafoussam-Bamenda", 5.4737, 10.4177, 5.9597, 10.1453, 80));

        // Corridor 13: Douala -> Nkongsamba (145 km) - 100 arrêts
        pois.addAll(generateStopsAlongRoute("Route Douala-Nkongsamba", 4.0530, 9.7000, 4.9547, 9.9383, 100));

        // Corridor 14: Yaoundé -> Edéa (Sud, 65 km) - 60 arrêts
        pois.addAll(generateStopsAlongRoute("Route Yaoundé-Edéa", 3.8480, 11.5021, 3.7967, 10.1333, 60));

        // Corridor 15: Edéa -> Douala (100 km) - 80 arrêts
        pois.addAll(generateStopsAlongRoute("Route Edéa-Douala", 3.7967, 10.1333, 4.0530, 9.7000, 80));

        // Corridor 16: Kumba -> Limbé (via Tiko, 60 km) - 60 arrêts
        pois.addAll(generateStopsAlongRoute("Route Kumba-Limbé", 4.6333, 9.4500, 4.0167, 9.2000, 60));

        // Corridor 17: Bertoua -> Abong-Mbang (Est, 60 km) - 50 arrêts
        pois.addAll(generateStopsAlongRoute("Route Bertoua-Abong-Mbang", 4.5767, 13.6783, 3.9833, 13.1833, 50));

        // Corridor 18: Bafoussam -> Dschang (Ouest, 55 km) - 50 arrêts
        pois.addAll(generateStopsAlongRoute("Route Bafoussam-Dschang", 5.4737, 10.4177, 5.4500, 10.0667, 50));

        log.info("Génération de {} POIs au total pour le Cameroun.", pois.size());

        return poiRepository.saveAll(pois).then();
    }

    // Méthode utilitaire pour générer des arrêts le long d'une route
    private List<Poi> generateStopsAlongRoute(String routeName, double startLat, double startLon, double endLat,
            double endLon, int numStops) {
        List<Poi> stops = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < numStops; i++) {
            // Interpolation linéaire avec variation aléatoire
            double t = (double) i / numStops;
            double baseLat = startLat + (endLat - startLat) * t;
            double baseLon = startLon + (endLon - startLon) * t;

            // Ajout d'une variation aléatoire (±0.01 degrés soit ~1 km)
            double lat = baseLat + (random.nextDouble() - 0.5) * 0.02;
            double lon = baseLon + (random.nextDouble() - 0.5) * 0.02;

            String stopName = routeName + " - Arrêt " + (i + 1);
            String cityEstimate = estimateCityFromRoute(routeName, t);

            stops.add(createPoi(stopName, "Point d'arrêt bus", lat, lon, cityEstimate, "Transport"));
        }

        return stops;
    }

    // Estimation de la ville en fonction de la progression sur la route
    private String estimateCityFromRoute(String routeName, double progress) {
        if (routeName.contains("Yaoundé-Douala")) {
            if (progress < 0.2)
                return "Yaoundé";
            if (progress < 0.4)
                return "Edéa";
            if (progress < 0.6)
                return "Nkongsamba";
            if (progress < 0.8)
                return "Bonabéri";
            return "Douala";
        } else if (routeName.contains("Yaoundé-Bafoussam")) {
            if (progress < 0.3)
                return "Yaoundé";
            if (progress < 0.7)
                return "Centre";
            return "Bafoussam";
        } else if (routeName.contains("Douala-Bafoussam")) {
            if (progress < 0.3)
                return "Douala";
            if (progress < 0.6)
                return "Nkongsamba";
            return "Bafoussam";
        } else if (routeName.contains("Yaoundé-Bertoua")) {
            if (progress < 0.4)
                return "Yaoundé";
            if (progress < 0.7)
                return "Belabo";
            return "Bertoua";
        } else if (routeName.contains("Douala-Kribi")) {
            if (progress < 0.5)
                return "Douala";
            return "Kribi";
        } else if (routeName.contains("Yaoundé-Ngaoundéré")) {
            if (progress < 0.3)
                return "Yaoundé";
            if (progress < 0.6)
                return "Belabo";
            return "Ngaoundéré";
        } else if (routeName.contains("Limbé")) {
            if (progress < 0.5)
                return "Douala";
            if (progress < 0.75)
                return "Buea";
            return "Limbé";
        } else if (routeName.contains("Kumba")) {
            if (progress < 0.7)
                return "Douala";
            return "Kumba";
        } else if (routeName.contains("Ebolowa")) {
            if (progress < 0.6)
                return "Yaoundé";
            return "Ebolowa";
        } else if (routeName.contains("Garoua-Maroua")) {
            if (progress < 0.5)
                return "Garoua";
            return "Maroua";
        } else if (routeName.contains("Ngaoundéré-Garoua")) {
            if (progress < 0.5)
                return "Ngaoundéré";
            return "Garoua";
        } else if (routeName.contains("Bamenda")) {
            if (progress < 0.5)
                return "Bafoussam";
            return "Bamenda";
        } else if (routeName.contains("Nkongsamba")) {
            if (progress < 0.6)
                return "Douala";
            return "Nkongsamba";
        } else if (routeName.contains("Edéa")) {
            if (progress < 0.5)
                return "Yaoundé";
            return "Edéa";
        } else if (routeName.contains("Dschang")) {
            if (progress < 0.5)
                return "Bafoussam";
            return "Dschang";
        }

        return "Cameroun"; // Valeur par défaut
    }

    private String determineCity(double fraction) {
        if (fraction < 0.1)
            return "Départ Zone";
        if (fraction > 0.9)
            return "Arrivée Zone";
        return "Village/Lieu-dit";
    }

    private Poi createPoi(String name, String desc, double lat, double lon, String city, String type) {
        return Poi.builder()
                .name(name)
                .description(desc)
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lon))
                .addressStreet(city)
                .addressCity(city)
                .phone("+237699000000") // Default
                .categoryId(9L)
                .reviewCount(0)
                .rating(BigDecimal.valueOf(3.0 + Math.random() * 2.0))
                .build();
    }
}
