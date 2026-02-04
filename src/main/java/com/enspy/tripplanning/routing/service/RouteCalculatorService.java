package com.enspy.tripplanning.routing.service;

import com.enspy.tripplanning.routing.dto.*;
import com.enspy.tripplanning.routing.model.RoadEdge;
import com.enspy.tripplanning.routing.model.RoadNode;
import com.enspy.tripplanning.routing.model.Route;
import com.enspy.tripplanning.routing.repository.RoadEdgeRepository;
import com.enspy.tripplanning.routing.repository.RoadNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.geo.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Service principal de calcul d'itinéraires.
 * 
 * Orchestre les différentes étapes:
 * 1. Snap des coordonnées GPS sur le graphe routier
 * 2. Appel à l'algorithme A*
 * 3. Construction de la réponse enrichie
 * 
 * Ce service fait le pont entre les DTOs (API) et les modèles métier.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2024-12-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCalculatorService {

    private final RoadNodeRepository nodeRepository;
    private final RoadEdgeRepository edgeRepository;
    private final AStarService aStarService;
    private final com.enspy.tripplanning.poi.repository.PoiRepository poiRepository;
    private final OsrmRoutingService osrmRoutingService;

    /**
     * Calcule un itinéraire à partir d'une requête utilisateur.
     * 
     * ÉTAPES:
     * 1. Trouver les nœuds du graphe les plus proches des coordonnées GPS
     * 2. Si des waypoints sont fournis, calculer les segments intermédiaires
     * 3. Calculer le chemin optimal avec A*
     * 4. Construire la réponse enrichie avec instructions
     * 
     * @param request Requête contenant départ et arrivée
     * @return Réponse avec itinéraire détaillé
     */
    public Mono<MultiRouteResponse> calculateRoute(RouteRequest request) {
        log.info("Calcul d'itinéraire multi-route demandé: ({}, {}) → ({}, {}) avec {} waypoints",
                request.getStartLatitude(), request.getStartLongitude(),
                request.getEndLatitude(), request.getEndLongitude(),
                request.getWaypointPoiIds() != null ? request.getWaypointPoiIds().size() : 0);

        return performRouteCalculation(request);
    }

    private Mono<MultiRouteResponse> performRouteCalculation(RouteRequest request) {
        // Points de départ et arrivée
        // Note: Point(x, y) = Point(longitude, latitude)
        org.springframework.data.geo.Point start = new org.springframework.data.geo.Point(request.getStartLongitude(),
                request.getStartLatitude());
        org.springframework.data.geo.Point end = new org.springframework.data.geo.Point(request.getEndLongitude(),
                request.getEndLatitude());

        // Récupérer les coordonnées des POIs pour la route personnalisée
        Mono<List<org.springframework.data.geo.Point>> waypointsMono;

        if (request.getWaypointPoiIds() != null && !request.getWaypointPoiIds().isEmpty()) {
            waypointsMono = Flux.fromIterable(request.getWaypointPoiIds())
                    .flatMap(poiId -> poiRepository.findById(poiId))
                    .map(poi -> new org.springframework.data.geo.Point(
                            poi.getLongitude() != null ? poi.getLongitude().doubleValue() : 0.0,
                            poi.getLatitude() != null ? poi.getLatitude().doubleValue() : 0.0))
                    .collectList();
        } else {
            waypointsMono = Mono.just(java.util.Collections.emptyList());
        }

        return waypointsMono.flatMap(waypoints -> {
            // 1. Calcul Route OPTIMALE (Directe sans waypoints)
            log.info("🟢 Appel OSRM pour route OPTIMALE (Directe)");
            Mono<RouteResponse> optimalRouteMono = osrmRoutingService.calculateRoute(start, end, null);

            // 2. Calcul Route PERSONNALISÉE (Avec waypoints)
            log.info("🔵 Appel OSRM pour route PERSONNALISÉE avec {} waypoints", waypoints.size());
            // Si pas de waypoints, la route personnalisée est techniquement identique à
            // l'optimale
            // mais on fait l'appel pour avoir la cohérence
            Mono<RouteResponse> customRouteMono = osrmRoutingService.calculateRoute(start, end, waypoints);

            return Mono.zip(customRouteMono, optimalRouteMono)
                    .map(tuple -> {
                        RouteResponse custom = tuple.getT1();
                        RouteResponse optimal = tuple.getT2();

                        return MultiRouteResponse.builder()
                                .found(custom.getFound() || optimal.getFound())
                                .customRoute(custom)
                                .optimalRoute(optimal)
                                .build();
                    });
        })
                .onErrorResume(error -> {
                    log.error("Erreur lors du calcul OSRM", error);
                    return Mono.just(MultiRouteResponse.builder()
                            .found(false)
                            .errorMessage("Erreur OSRM: " + error.getMessage())
                            .build());
                });
    }

    private Mono<double[]> getBoundingBox(RouteRequest request) {
        // Commencer avec les points de départ et d'arrivée
        double minLat = Math.min(request.getStartLatitude(), request.getEndLatitude());
        double maxLat = Math.max(request.getStartLatitude(), request.getEndLatitude());
        double minLon = Math.min(request.getStartLongitude(), request.getEndLongitude());
        double maxLon = Math.max(request.getStartLongitude(), request.getEndLongitude());

        if (request.getWaypointPoiIds() == null || request.getWaypointPoiIds().isEmpty()) {
            double padding = 0.5; // Increased from 0.1 to 0.5 for Cameroon sparse network
            return Mono.just(new double[] {
                    minLat - padding,
                    minLon - padding,
                    maxLat + padding,
                    maxLon + padding
            });
        }

        // Inclure les POIs dans le bounding box
        return Flux.fromIterable(request.getWaypointPoiIds())
                .flatMap(poiRepository::findById)
                .reduce(new double[] { minLat, minLon, maxLat, maxLon }, (box, poi) -> {
                    box[0] = Math.min(box[0], poi.getLatitude().doubleValue());
                    box[1] = Math.min(box[1], poi.getLongitude().doubleValue());
                    box[2] = Math.max(box[2], poi.getLatitude().doubleValue());
                    box[3] = Math.max(box[3], poi.getLongitude().doubleValue());
                    return box;
                })
                .map(box -> {
                    // Ajouter un "padding" de 0.5 degré (~55km) pour permettre des détours
                    // Augmenté car le réseau national camerounais est peu dense
                    double padding = 0.5;
                    return new double[] {
                            box[0] - padding,
                            box[1] - padding,
                            box[2] + padding,
                            box[3] + padding
                    };
                });
    }

    private Mono<RouteResponse> calculateCustomRoute(RouteRequest request,
            com.enspy.tripplanning.routing.model.Subgraph subgraph) {
        if (request.getWaypointPoiIds() == null || request.getWaypointPoiIds().isEmpty()) {
            return calculateSimpleRoute(request, subgraph);
        }
        // ÉTAPE: Récupérer les POIs pour les waypoints
        return Flux.fromIterable(request.getWaypointPoiIds())
                .flatMap(poiRepository::findById)
                .collectList()
                .flatMap(pois -> {
                    // S'assurer que les POIs sont dans l'ordre demandé
                    List<com.enspy.tripplanning.poi.entity.Poi> orderedPois = request.getWaypointPoiIds().stream()
                            .map(id -> pois.stream().filter(p -> p.getPoiId().equals(id)).findFirst().orElse(null))
                            .filter(Objects::nonNull)
                            .toList();

                    // Construire la liste des points de passage: Start -> POI1 -> POI2 -> ... ->
                    // End
                    List<Mono<RoadNode>> nodeMonos = new ArrayList<>();
                    nodeMonos.add(snapToNearestNode(request.getStartLatitude(), request.getStartLongitude())
                            .doOnNext(node -> log.info("🏁 Start node snapped to: {} (id: {})", node.getName(),
                                    node.getNodeId())));

                    for (int i = 0; i < orderedPois.size(); i++) {
                        com.enspy.tripplanning.poi.entity.Poi poi = orderedPois.get(i);
                        int index = i;
                        nodeMonos.add(
                                snapToNearestNode(poi.getLatitude().doubleValue(), poi.getLongitude().doubleValue())
                                        .doOnNext(node -> log.info("📍 Waypoint {} ({}) snapped to: {} (id: {})",
                                                index + 1, poi.getName(), node.getName(), node.getNodeId())));
                    }

                    nodeMonos.add(snapToNearestNode(request.getEndLatitude(), request.getEndLongitude())
                            .doOnNext(node -> log.info("🏁 End node snapped to: {} (id: {})", node.getName(),
                                    node.getNodeId())));

                    return Flux.fromIterable(nodeMonos)
                            .concatMap(m -> m)
                            .collectList()
                            .doOnNext(list -> log.info("Successfully snapped {} of {} requested points", list.size(),
                                    nodeMonos.size()));
                })
                .flatMap(roadNodes -> {
                    // Calculer les segments entre chaque paire de nœuds consécutifs
                    List<Mono<Route>> routeMonos = new ArrayList<>();
                    for (int i = 0; i < roadNodes.size() - 1; i++) {
                        routeMonos.add(aStarService.calculateShortestPath(roadNodes.get(i).getNodeId(),
                                roadNodes.get(i + 1).getNodeId(), subgraph));
                    }

                    return Mono.zip(routeMonos, routes -> {
                        List<Route> routeSegments = new ArrayList<>();
                        for (Object r : routes) {
                            routeSegments.add((Route) r);
                        }
                        return routeSegments;
                    });
                })
                .flatMap(segments -> enrichRouteSegments(segments)
                        .map(enrichedSegments -> mergeRoutes(enrichedSegments, request)));
    }

    private Mono<List<Route>> enrichRouteSegments(List<Route> segments) {
        List<Long> allNodeIds = segments.stream()
                .flatMap(r -> r.getNodes().stream())
                .map(RoadNode::getNodeId)
                .distinct()
                .toList();

        List<Long> allEdgeIds = segments.stream()
                .flatMap(r -> r.getEdges().stream())
                .map(RoadEdge::getEdgeId)
                .distinct()
                .toList();

        return Mono.zip(
                nodeRepository.findAllById(allNodeIds).collectList(),
                edgeRepository.findAllById(allEdgeIds).collectList()).map(tuple -> {
                    java.util.Map<Long, RoadNode> nodeMap = tuple.getT1().stream()
                            .collect(java.util.stream.Collectors.toMap(RoadNode::getNodeId, n -> n));
                    java.util.Map<Long, RoadEdge> edgeMap = tuple.getT2().stream()
                            .collect(java.util.stream.Collectors.toMap(RoadEdge::getEdgeId, e -> e));

                    for (Route r : segments) {
                        // Enrich Nodes
                        List<RoadNode> nodes = r.getNodes();
                        for (int i = 0; i < nodes.size(); i++) {
                            RoadNode enriched = nodeMap.get(nodes.get(i).getNodeId());
                            if (enriched != null)
                                nodes.set(i, enriched);
                        }

                        // Enrich Edges
                        List<RoadEdge> edges = r.getEdges();
                        for (int i = 0; i < edges.size(); i++) {
                            RoadEdge enriched = edgeMap.get(edges.get(i).getEdgeId());
                            if (enriched != null)
                                edges.set(i, enriched);
                        }

                        if (r.getStartNode() != null)
                            r.setStartNode(nodeMap.get(r.getStartNode().getNodeId()));
                        if (r.getEndNode() != null)
                            r.setEndNode(nodeMap.get(r.getEndNode().getNodeId()));

                        // Recalculate metrics based on enriched edges
                        r.setTotalDistanceKm(r.calculateTotalDistance());
                        r.setTotalTimeSeconds(r.calculateTotalTime());
                    }
                    return segments;
                });
    }

    private Mono<RouteResponse> calculateSimpleRoute(RouteRequest request,
            com.enspy.tripplanning.routing.model.Subgraph subgraph) {
        log.info("📍 Starting simple route calculation: ({}, {}) -> ({}, {})",
                request.getStartLatitude(), request.getStartLongitude(),
                request.getEndLatitude(), request.getEndLongitude());

        return Mono.zip(
                snapToNearestNode(request.getStartLatitude(), request.getStartLongitude()),
                snapToNearestNode(request.getEndLatitude(), request.getEndLongitude()))
                .flatMap(tuple -> {
                    RoadNode startNode = tuple.getT1();
                    RoadNode endNode = tuple.getT2();

                    log.info("✅ Nodes snapped: {} -> {}", startNode.getName(), endNode.getName());

                    return aStarService.calculateShortestPath(startNode.getNodeId(), endNode.getNodeId(), subgraph)
                            .flatMap(route -> {
                                if (!route.getFound()) {
                                    log.warn("❌ A* failed to find path between {} and {}", startNode.getName(),
                                            endNode.getName());
                                }
                                return enrichRouteSegments(List.of(route));
                            })
                            .map(enrichedList -> buildResponse(enrichedList.get(0), request));
                });
    }

    private RouteResponse mergeRoutes(List<Route> rawSegments, RouteRequest request) {
        if (rawSegments.stream().anyMatch(r -> !r.isValid())) {
            String errorMsg = rawSegments.stream()
                    .filter(r -> !r.isValid())
                    .map(Route::getErrorMessage)
                    .collect(java.util.stream.Collectors.joining(" | "));
            return RouteResponse.builder().found(false).errorMessage(errorMsg).build();
        }

        // Fusionner les données
        double totalDist = 0;
        int totalTime = 0;
        List<RoadNode> allNodes = new ArrayList<>();
        List<RoadEdge> allEdges = new ArrayList<>();
        long totalExplored = 0;
        long totalCompTime = 0;

        for (int i = 0; i < rawSegments.size(); i++) {
            Route r = rawSegments.get(i);
            totalDist += r.getTotalDistanceKm();
            totalTime += r.getTotalTimeSeconds();
            totalExplored += r.getNodesExplored();
            totalCompTime += r.getComputationTimeMs();

            // Ajouter les nœuds (éviter les doublons aux jonctions)
            if (i == 0) {
                allNodes.addAll(r.getNodes());
            } else {
                List<RoadNode> subNodes = r.getNodes();
                if (!subNodes.isEmpty()) {
                    allNodes.addAll(subNodes.subList(1, subNodes.size()));
                }
            }
            allEdges.addAll(r.getEdges());
        }

        Route mergedRoute = Route.builder()
                .found(true)
                .startNode(rawSegments.get(0).getStartNode())
                .endNode(rawSegments.get(rawSegments.size() - 1).getEndNode())
                .nodes(allNodes)
                .edges(allEdges)
                .totalDistanceKm(totalDist)
                .totalTimeSeconds(totalTime)
                .nodesExplored((int) totalExplored)
                .computationTimeMs(totalCompTime)
                .build();

        return buildResponse(mergedRoute, request);
    }

    /**
     * Trouve le nœud du graphe le plus proche d'une coordonnée GPS.
     * 
     * C'est l'opération de "snapping" qui projette un point GPS
     * sur le réseau routier.
     * 
     * TECHNIQUE: Utilise l'index spatial PostGIS pour trouver
     * le nœud le plus proche en O(log V) avec l'index R-Tree.
     * 
     * @param latitude  Latitude GPS
     * @param longitude Longitude GPS
     * @return Nœud le plus proche
     */
    private Mono<RoadNode> snapToNearestNode(Double latitude, Double longitude) {
        return nodeRepository.findNearestNode(latitude, longitude)
                .switchIfEmpty(Mono.error(new RuntimeException(
                        String.format("Aucun nœud trouvé près de (%.4f, %.4f)", latitude, longitude))))
                .doOnNext(node -> log.debug("Point (%.4f, %.4f) snappé sur nœud {} (distance: %.2f km)",
                        latitude, longitude, node.getName(),
                        calculateDistance(latitude, longitude, node.getLatitude(), node.getLongitude())));
    }

    /**
     * Construit la réponse enrichie à partir de la route calculée.
     * 
     * Transforme le résultat brut de A* en un DTO lisible
     * avec instructions de navigation.
     * 
     * @param route   Route calculée par A*
     * @param request Requête originale
     * @return Réponse formatée
     */
    private RouteResponse buildResponse(Route route, RouteRequest request) {
        if (!route.isValid()) {
            return RouteResponse.builder()
                    .found(false)
                    .errorMessage(route.getErrorMessage())
                    .build();
        }

        // Construire les points
        RoutePointDTO startPoint = buildRoutePoint(route.getStartNode(), "start");
        RoutePointDTO endPoint = buildRoutePoint(route.getEndNode(), "end");

        // Construire les segments
        List<RouteSegmentDTO> segments = buildSegments(route);

        // Construire les instructions
        List<String> instructions = route.getNavigationInstructions();

        // Statistiques
        RouteStatisticsDTO stats = RouteStatisticsDTO.builder()
                .nodesExplored(route.getNodesExplored())
                .computationTimeMs(route.getComputationTimeMs())
                .algorithm("A* avec heuristique euclidienne")
                .build();

        // Construire la géométrie encodée (JSON Array [[lat, lng], ...])
        StringBuilder geomBuilder = new StringBuilder("[");
        List<RoadNode> nodesData = route.getNodes();
        for (int i = 0; i < nodesData.size(); i++) {
            RoadNode node = nodesData.get(i);
            geomBuilder.append(String.format(Locale.US, "[%.6f,%.6f]", node.getLatitude(), node.getLongitude()));
            if (i < nodesData.size() - 1)
                geomBuilder.append(",");
        }
        geomBuilder.append("]");

        return RouteResponse.builder()
                .found(true)
                .start(startPoint)
                .end(endPoint)
                .segments(segments)
                .totalDistanceKm(route.getTotalDistanceKm())
                .totalTimeSeconds(route.getTotalTimeSeconds())
                .formattedTime(route.getFormattedTime())
                .segmentCount(route.getSegmentCount())
                .instructions(instructions)
                .statistics(stats)
                .geometryEncoded(geomBuilder.toString())
                .build();
    }

    /**
     * Construit un DTO de point à partir d'un nœud.
     * 
     * @param node Nœud du graphe
     * @param type Type de point (start/end/waypoint)
     * @return DTO du point
     */
    private RoutePointDTO buildRoutePoint(RoadNode node, String type) {
        return RoutePointDTO.builder()
                .nodeId(node.getNodeId())
                .latitude(node.getLatitude())
                .longitude(node.getLongitude())
                .name(node.getName() != null ? node.getName() : "Point " + type)
                .type(type)
                .build();
    }

    /**
     * Construit la liste des segments à partir de la route.
     * 
     * @param route Route calculée
     * @return Liste des segments enrichis
     */
    private List<RouteSegmentDTO> buildSegments(Route route) {
        List<RouteSegmentDTO> segments = new ArrayList<>();
        List<RoadNode> nodes = route.getNodes();
        List<RoadEdge> edges = route.getEdges();

        for (int i = 0; i < edges.size(); i++) {
            RoadEdge edge = edges.get(i);
            RoadNode startNode = nodes.get(i);
            RoadNode endNode = nodes.get(i + 1);

            RouteSegmentDTO segment = RouteSegmentDTO.builder()
                    .segmentNumber(i + 1)
                    .edgeId(edge.getEdgeId())
                    .streetName(edge.getStreetName())
                    .roadType(edge.getRoadType())
                    .distanceKm(edge.getDistanceMetersOrCalculate() / 1000.0)
                    .timeSeconds(edge.getTravelTimeSeconds())
                    .maxSpeedKmh(edge.getMaxSpeedKmh())
                    .startPoint(buildRoutePoint(startNode, "segment_start"))
                    .endPoint(buildRoutePoint(endNode, "segment_end"))
                    .instruction(buildInstruction(edge, i + 1))
                    .build();

            segments.add(segment);
        }

        return segments;
    }

    /**
     * Génère une instruction de navigation pour un segment.
     * 
     * @param edge          Arête du segment
     * @param segmentNumber Numéro du segment
     * @return Instruction textuelle
     */
    private String buildInstruction(RoadEdge edge, int segmentNumber) {
        String streetName = edge.getStreetName() != null ? edge.getStreetName() : "la route";
        double distanceKm = edge.getDistanceMetersOrCalculate() / 1000.0;
        int timeMin = edge.getTravelTimeSeconds() != null ? edge.getTravelTimeSeconds() / 60 : 0;

        return String.format(
                "%d. Suivez %s pendant %.1f km (%d min)",
                segmentNumber,
                streetName,
                distanceKm,
                timeMin);
    }

    /**
     * Calcule la distance euclidienne entre deux points GPS.
     * 
     * @param lat1 Latitude point 1
     * @param lon1 Longitude point 1
     * @param lat2 Latitude point 2
     * @param lon2 Longitude point 2
     * @return Distance en kilomètres
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double lon1Rad = Math.toRadians(lon1);
        double lon2Rad = Math.toRadians(lon2);

        double angle = Math.acos(
                Math.sin(lat1Rad) * Math.sin(lat2Rad) +
                        Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.cos(lon2Rad - lon1Rad));

        return EARTH_RADIUS_KM * angle;
    }
}