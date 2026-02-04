package com.enspy.tripplanning.routing.service;

import com.enspy.tripplanning.routing.model.RoadEdge;
import com.enspy.tripplanning.routing.model.RoadNode;
import com.enspy.tripplanning.routing.model.Route;
import com.enspy.tripplanning.routing.repository.RoadEdgeRepository;
import com.enspy.tripplanning.routing.repository.RoadNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

/**
 * Implémentation de l'algorithme A* pour le calcul de plus court chemin.
 * 
 * THÉORIE (selon Modélisation Mathématique, page 5):
 * ================================================
 * 
 * A* est une extension de Dijkstra utilisant une heuristique h(v)
 * pour guider la recherche vers la destination.
 * 
 * Pour chaque nœud v, on calcule:
 * - g(v) = coût réel depuis le départ
 * - h(v) = estimation du coût jusqu'à l'arrivée (heuristique)
 * - f(v) = g(v) + h(v) = coût total estimé
 * 
 * HEURISTIQUE UTILISÉE:
 * h(v) = distance_euclidienne(v, destination) / vitesse_max
 * 
 * Cette heuristique est ADMISSIBLE car:
 * - La distance euclidienne ≤ distance réelle par route
 * - Diviser par vitesse_max donne le temps minimal théorique
 * - Donc h(v) ≤ coût réel, ce qui garantit l'optimalité
 * 
 * COMPLEXITÉ:
 * - Temporelle: O(E log V) avec tas binaire
 * - Spatiale: O(V) pour stocker g, f, cameFrom
 * - Pratique: explore O(√V) nœuds grâce à l'heuristique
 * 
 * PERFORMANCE ATTENDUE (selon doc page 7):
 * - Trajet < 100 km : 10-50 ms
 * - Trajet 100-500 km : 100-500 ms
 * - Trajet > 500 km : 500-2000 ms
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2024-12-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AStarService {

    private final RoadNodeRepository nodeRepository;
    private final RoadEdgeRepository edgeRepository;
    private final RoutingOptimizationService optimizationService;

    /**
     * Vitesse maximale théorique pour l'heuristique (km/h)
     * Correspond à la vitesse sur autoroute
     */
    private static final double MAX_SPEED_KMH = 130.0;

    /**
     * Calcule le plus court chemin entre deux nœuds en utilisant A*.
     * 
     * ALGORITHME (pseudo-code page 6 de la modélisation):
     * 
     * 1. Initialiser g[start] = 0, f[start] = h(start)
     * 2. Ajouter start à openSet (file de priorité sur f)
     * 3. Tant que openSet n'est pas vide:
     * a. Extraire u = nœud avec f minimal
     * b. Si u == destination → reconstruire et retourner chemin
     * c. Pour chaque voisin v de u:
     * - Calculer g_temp = g[u] + w(u,v)
     * - Si g_temp < g[v]:
     * * Mettre à jour g[v], f[v]
     * * Ajouter v à openSet
     * 4. Si openSet vide → aucun chemin trouvé
     * 
     * @param startNodeId ID du nœud de départ
     * @param endNodeId   ID du nœud d'arrivée
     * @return Route calculée (ou vide si aucun chemin)
     */
    public Mono<Route> calculateShortestPath(Long startNodeId, Long endNodeId) {
        return calculateShortestPath(startNodeId, endNodeId, null);
    }

    /**
     * Calcule le plus court chemin avec un sous-graphe optionnel pré-chargé.
     */
    public Mono<Route> calculateShortestPath(Long startNodeId, Long endNodeId,
            com.enspy.tripplanning.routing.model.Subgraph subgraph) {
        long startTime = System.currentTimeMillis();

        log.debug("Calcul A* : {} → {}", startNodeId, endNodeId);

        return Mono.zip(
                nodeRepository.findById(startNodeId),
                nodeRepository.findById(endNodeId))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(tuple -> {
                    RoadNode start = tuple.getT1();
                    RoadNode end = tuple.getT2();

                    // Exécuter A* (avec ou sans sous-graphe)
                    Route route = executeAStar(start, end, subgraph);

                    long computationTime = System.currentTimeMillis() - startTime;
                    route.setComputationTimeMs(computationTime);

                    log.info("A* terminé en {}ms - Chemin trouvé: {}, Distance: {} km, Nœuds explorés: {}",
                            computationTime, route.getFound(), route.getTotalDistanceKm(), route.getNodesExplored());

                    return Mono.just(route);
                })
                .switchIfEmpty(Mono.just(Route.builder()
                        .found(false)
                        .errorMessage("Nœud de départ ou d'arrivée introuvable")
                        .build()))
                .onErrorResume(error -> {
                    log.error("Erreur lors du calcul A*", error);
                    return Mono.just(Route.builder()
                            .found(false)
                            .errorMessage("Erreur: " + error.getMessage())
                            .build());
                });
    }

    /**
     * Charge une portion du graphe en mémoire.
     */
    public Mono<com.enspy.tripplanning.routing.model.Subgraph> fetchSubgraph(Double minLat, Double minLon,
            Double maxLat, Double maxLon) {
        // Ajouter une marge de sécurité (0.5 deg ~ 55km pour le réseau camerounais
        // sparse)
        double margin = 0.5;
        Double fMinLat = minLat - margin;
        Double fMinLon = minLon - margin;
        Double fMaxLat = maxLat + margin;
        Double fMaxLon = maxLon + margin;

        log.info("Chargement du sous-graphe en mémoire [{}, {}] -> [{}, {}]", fMinLat, fMinLon, fMaxLat, fMaxLon);

        return edgeRepository.findEdgesInBoundingBox(fMinLat, fMinLon, fMaxLat, fMaxLon)
                .collectList()
                .flatMap(edges -> {
                    // Collecter tous les IDs de nœuds référencés par les arêtes
                    java.util.Set<Long> nodeIds = new java.util.HashSet<>();
                    for (RoadEdge edge : edges) {
                        nodeIds.add(edge.getSourceNodeId());
                        nodeIds.add(edge.getTargetNodeId());
                    }

                    // Charger aussi les nœuds qui sont dans la bbox (même sans arêtes)
                    // pour s'assurer que les points de snapping sont présents
                    return nodeRepository.findNodesInBoundingBox(fMinLat, fMinLon, fMaxLat, fMaxLon)
                            .collectList()
                            .flatMap(nodesInBox -> {
                                for (RoadNode node : nodesInBox) {
                                    nodeIds.add(node.getNodeId());
                                }

                                // Charger tous les nœuds par ID
                                return nodeRepository.findAllById(nodeIds).collectList()
                                        .map(allNodes -> {
                                            com.enspy.tripplanning.routing.model.Subgraph subgraph = new com.enspy.tripplanning.routing.model.Subgraph();
                                            subgraph.index(allNodes, edges);
                                            log.info("Sous-graphe indexé: {} nœuds, {} arêtes", allNodes.size(),
                                                    edges.size());
                                            return subgraph;
                                        });
                            });
                });
    }

    private Route executeAStar(RoadNode start, RoadNode end, com.enspy.tripplanning.routing.model.Subgraph subgraph) {
        Map<Long, Double> g = new HashMap<>();
        Map<Long, Double> f = new HashMap<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        PriorityQueue<NodeScore> openSet = new PriorityQueue<>(Comparator.comparingDouble(NodeScore::fScore));
        Set<Long> closedSet = new HashSet<>();

        g.put(start.getNodeId(), 0.0);
        f.put(start.getNodeId(), heuristic(start, end));
        openSet.add(new NodeScore(start.getNodeId(), f.get(start.getNodeId())));

        int nodesExplored = 0;

        while (!openSet.isEmpty()) {
            NodeScore current = openSet.poll();
            Long currentId = current.nodeId;

            if (closedSet.contains(currentId))
                continue;
            nodesExplored++;
            closedSet.add(currentId);

            RoadNode currentNode;
            if (subgraph != null) {
                currentNode = subgraph.getNode(currentId);
            } else {
                currentNode = nodeRepository.findById(currentId).block();
            }

            if (currentNode == null) {
                log.warn("Nœud {} non trouvé dans le graphe/sous-graphe", currentId);
                continue;
            }

            log.debug("A* - Nœud exploré: {} ({}) - g={}", currentId, currentNode.getName(), g.get(currentId));

            if (currentId.equals(end.getNodeId())) {
                log.info("🎯 Destination atteinte! ID: {}, Nom: {}", currentId, currentNode.getName());
                return reconstructPath(cameFrom, start, end, nodesExplored, subgraph);
            }

            // Récupérer les voisins
            List<RoadEdge> neighborEdges;
            if (subgraph != null) {
                neighborEdges = subgraph.getNeighbors(currentId);
            } else {
                neighborEdges = edgeRepository.findNeighborEdges(currentId).collectList().block();
            }

            if (neighborEdges == null || neighborEdges.isEmpty()) {
                log.debug("   ↳ Aucun voisin pour le nœud {}", currentId);
                continue;
            }

            log.debug("   ↳ {} voisins trouvés", neighborEdges.size());

            for (RoadEdge edge : neighborEdges) {
                Long neighborId = getNeighborId(edge, currentId);
                if (neighborId == null || closedSet.contains(neighborId))
                    continue;

                // --- OPTIMISATION MULTI-NIVEAUX DESACTIVEE POUR LE MOMENT ---
                // Si on est loin du départ et de l'arrivée, on ne garde que les axes principaux
                // double distFromStart = calculateHaversine(
                // currentNode.getLatitude(), currentNode.getLongitude(),
                // start.getLatitude(), start.getLongitude());
                // double distFromEnd = calculateHaversine(
                // currentNode.getLatitude(), currentNode.getLongitude(),
                // end.getLatitude(), end.getLongitude());

                // if (!optimizationService.shouldExploreEdge(edge, distFromStart, distFromEnd))
                // {
                // continue; // Skip les petites routes au milieu du trajet
                // }
                // -----------------------------------------------------

                Integer travelTime = edge.getTravelTimeSeconds();
                if (travelTime == null || travelTime <= 0) {
                    // Calculer le temps si manquant
                    travelTime = edge.calculateTravelTime();
                }
                double tentativeG = g.get(currentId) + travelTime;

                if (tentativeG < g.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    log.debug("      → Nouveau meilleur chemin pour le voisin {}: g={}", neighborId, tentativeG);
                    cameFrom.put(neighborId, currentId);
                    g.put(neighborId, tentativeG);

                    RoadNode neighborNode = (subgraph != null) ? subgraph.getNode(neighborId)
                            : nodeRepository.findById(neighborId).block();
                    if (neighborNode != null) {
                        double fScore = tentativeG + heuristic(neighborNode, end);
                        f.put(neighborId, fScore);
                        openSet.add(new NodeScore(neighborId, fScore));
                    } else {
                        log.warn("      ⚠️ Voisin {} introuvable dans le graphe!", neighborId);
                    }
                }
            }
        }

        log.warn("❌ Aucun chemin trouvé entre {} et {} après exploration de {} nœuds",
                start.getName(), end.getName(), nodesExplored);

        return Route.builder()
                .startNode(start)
                .endNode(end)
                .found(false)
                .nodesExplored(nodesExplored)
                .errorMessage("Aucun chemin trouvé")
                .build();
    }

    /**
     * Calcule l'heuristique h(v) = estimation du coût restant.
     * 
     * TECHNIQUE: Distance Haversine (plus précise que la distance euclidienne
     * sur une sphère).
     * 
     * FORMULE:
     * h(v) = distance_haversine(v, destination) / vitesse_max
     * 
     * @param current Nœud actuel
     * @param goal    Nœud destination
     * @return Estimation du temps restant en secondes
     */
    private double heuristic(RoadNode current, RoadNode goal) {
        double distanceKm = calculateHaversine(
                current.getLatitude(), current.getLongitude(),
                goal.getLatitude(), goal.getLongitude());

        // Temps théorique minimum en heures
        double timeHours = distanceKm / MAX_SPEED_KMH;

        // Conversion en secondes
        return timeHours * 3600;
    }

    /**
     * Formule de Haversine pour calculer la distance entre deux points GPS.
     */
    private double calculateHaversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Rayon de la Terre en km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Reconstruit le chemin optimal depuis la table cameFrom.
     * 
     * COMPLEXITÉ: O(k) où k = longueur du chemin
     * 
     * @param cameFrom      Table des prédécesseurs
     * @param start         Nœud de départ
     * @param end           Nœud d'arrivée
     * @param nodesExplored Nombre de nœuds explorés
     * @return Route complète
     */
    private Route reconstructPath(
            Map<Long, Long> cameFrom,
            RoadNode start,
            RoadNode end,
            int nodesExplored,
            com.enspy.tripplanning.routing.model.Subgraph subgraph) {
        List<RoadNode> pathNodes = new ArrayList<>();
        List<RoadEdge> pathEdges = new ArrayList<>();

        Long current = end.getNodeId();
        pathNodes.add(0, end);

        while (cameFrom.containsKey(current)) {
            Long previous = cameFrom.get(current);

            // Trouver l'arête (depuis le sous-graphe ou la DB)
            RoadEdge edge;
            if (subgraph != null) {
                Long finalPrevious = previous;
                Long finalCurrent = current;
                edge = subgraph.getNeighbors(previous).stream()
                        .filter(e -> getNeighborId(e, finalPrevious).equals(finalCurrent))
                        .findFirst()
                        .orElse(null);
            } else {
                edge = edgeRepository
                        .findBySourceNodeIdAndTargetNodeId(previous, current)
                        .switchIfEmpty(edgeRepository.findBySourceNodeIdAndTargetNodeId(current, previous))
                        .block();
            }

            if (edge != null) {
                pathEdges.add(0, edge);
            }

            RoadNode prevNode = (subgraph != null) ? subgraph.getNode(previous)
                    : nodeRepository.findById(previous).block();
            if (prevNode != null) {
                pathNodes.add(0, prevNode);
            }

            current = previous;
        }

        Route route = Route.builder()
                .startNode(start)
                .endNode(end)
                .nodes(pathNodes)
                .edges(pathEdges)
                .found(true)
                .nodesExplored(nodesExplored)
                .build();

        route.setTotalDistanceKm(route.calculateTotalDistance());
        route.setTotalTimeSeconds(route.calculateTotalTime());

        return route;
    }

    /**
     * Détermine l'ID du voisin à partir d'une arête.
     * 
     * @param edge      Arête
     * @param currentId ID du nœud actuel
     * @return ID du voisin
     */
    private Long getNeighborId(RoadEdge edge, Long currentId) {
        if (edge.getSourceNodeId().equals(currentId)) {
            return edge.getTargetNodeId();
        } else {
            return edge.getSourceNodeId();
        }
    }

    /**
     * Classe interne pour stocker un nœud avec son f-score.
     * Utilisée dans la PriorityQueue.
     */
    private static class NodeScore {
        final Long nodeId;
        final double fScore;

        NodeScore(Long nodeId, double fScore) {
            this.nodeId = nodeId;
            this.fScore = fScore;
        }

        double fScore() {
            return fScore;
        }
    }
}