package com.enspy.tripplanning.routing.repository;

import com.enspy.tripplanning.routing.model.RoadEdge;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository réactif pour les arêtes du graphe routier.
 * 
 * Fournit les requêtes essentielles pour l'algorithme A* :
 * - Récupération des voisins d'un nœud
 * - Navigation dans le graphe
 * - Filtrage par type de route
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2024-12-15
 */
@Repository
public interface RoadEdgeRepository extends R2dbcRepository<RoadEdge, Long> {

        /**
         * Trouve toutes les arêtes sortantes d'un nœud.
         * 
         * CRITIQUE pour l'algorithme A* : cette requête est appelée
         * pour chaque nœud exploré lors du pathfinding.
         * 
         * Performance attendue : < 1ms avec index sur source_node_id
         * 
         * Pour un nœud u, retourne toutes les arêtes (u, v) ∈ E
         * 
         * @param sourceNodeId ID du nœud source
         * @return Flux d'arêtes sortantes
         */
        Flux<RoadEdge> findBySourceNodeId(Long sourceNodeId);

        /**
         * Trouve toutes les arêtes entrantes d'un nœud.
         * 
         * Utile pour :
         * - A* bidirectionnel (recherche depuis la destination)
         * - Analyse du graphe inversé
         * 
         * Pour un nœud v, retourne toutes les arêtes (u, v) ∈ E
         * 
         * @param targetNodeId ID du nœud cible
         * @return Flux d'arêtes entrantes
         */
        Flux<RoadEdge> findByTargetNodeId(Long targetNodeId);

        /**
         * Trouve l'arête directe entre deux nœuds (si elle existe).
         * 
         * Utile pour vérifier l'existence d'une connexion directe.
         * 
         * @param sourceNodeId ID du nœud source
         * @param targetNodeId ID du nœud cible
         * @return Arête si elle existe
         */
        Mono<RoadEdge> findBySourceNodeIdAndTargetNodeId(
                        Long sourceNodeId,
                        Long targetNodeId);

        /**
         * Trouve les arêtes par identifiant OSM.
         * Plusieurs arêtes peuvent partager le même OSM ID
         * (routes bidirectionnelles = 2 arêtes).
         * 
         * @param osmWayId Identifiant OpenStreetMap
         * @return Flux d'arêtes
         */
        Flux<RoadEdge> findByOsmWayId(Long osmWayId);

        /**
         * Trouve les arêtes par type de route.
         * 
         * Permet de filtrer par qualité de route :
         * - "motorway" pour les autoroutes uniquement
         * - "primary", "secondary" pour les routes principales
         * 
         * @param roadType Type de route
         * @return Flux d'arêtes du type spécifié
         */
        Flux<RoadEdge> findByRoadType(String roadType);

        /**
         * Trouve toutes les arêtes à sens unique.
         * Utile pour analyse et statistiques.
         * 
         * @return Flux d'arêtes unidirectionnelles
         */
        @Query("SELECT * FROM road_edges WHERE one_way = true")
        Flux<RoadEdge> findAllOneWayEdges();

        /**
         * Trouve les voisins d'un nœud avec leurs arêtes.
         * 
         * Cette requête est au cœur de l'algorithme A*.
         * Pour chaque nœud exploré, on récupère :
         * - Tous ses voisins accessibles
         * - Le coût (poids) pour y accéder
         * 
         * Prend en compte les routes bidirectionnelles :
         * - Si one_way = false, on ajoute aussi l'arête inverse
         * 
         * @param nodeId ID du nœud
         * @return Flux d'arêtes vers les voisins
         */
        @Query("""
                        SELECT * FROM road_edges
                        WHERE source_node_id = :nodeId

                        UNION

                        SELECT * FROM road_edges
                        WHERE target_node_id = :nodeId
                          AND one_way = false
                        """)
        Flux<RoadEdge> findNeighborEdges(@Param("nodeId") Long nodeId);

        /**
         * Compte le nombre d'arêtes sortantes d'un nœud.
         * Utile pour identifier les nœuds importants (hubs).
         * 
         * @param nodeId ID du nœud
         * @return Nombre d'arêtes sortantes
         */
        @Query("SELECT COUNT(*) FROM road_edges WHERE source_node_id = :nodeId")
        Mono<Long> countOutgoingEdges(@Param("nodeId") Long nodeId);

        /**
         * Trouve les arêtes dans un bounding box.
         * Charge une portion du graphe pour calculs locaux.
         * 
         * @param minLat Latitude minimale
         * @param minLon Longitude minimale
         * @param maxLat Latitude maximale
         * @param maxLon Longitude maximale
         * @return Flux d'arêtes dans la zone
         */
        @Query("""
                        SELECT edge_id, osm_way_id, source_node_id, target_node_id, distance_km, distance_meters, road_type, max_speed_kmh, travel_time_seconds, one_way, road_name, tags, created_at, updated_at
                        FROM road_edges
                        WHERE geom && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
                        """)
        Flux<RoadEdge> findEdgesInBoundingBox(
                        @Param("minLat") Double minLat,
                        @Param("minLon") Double minLon,
                        @Param("maxLat") Double maxLat,
                        @Param("maxLon") Double maxLon);

        /**
         * Trouve les arêtes les plus longues (pour analyse).
         * 
         * @param limit Nombre d'arêtes à retourner
         * @return Flux des arêtes les plus longues
         */
        @Query("""
                        SELECT * FROM road_edges
                        ORDER BY distance_meters DESC
                        LIMIT :limit
                        """)
        Flux<RoadEdge> findLongestEdges(@Param("limit") Integer limit);

        /**
         * Compte le nombre total d'arêtes dans le graphe.
         * 
         * @return Nombre total d'arêtes
         */
        @Query("SELECT COUNT(*) FROM road_edges")
        Mono<Long> countTotalEdges();

        /**
         * Calcule la longueur totale du réseau routier.
         * 
         * @return Distance totale en kilomètres
         */
        @Query("SELECT SUM(distance_meters) / 1000.0 FROM road_edges")
        Mono<Double> calculateTotalNetworkLength();

        /**
         * Trouve les arêtes avec une vitesse minimale.
         * Utile pour calculer des routes "rapides" uniquement.
         * 
         * @param minSpeed Vitesse minimale en km/h
         * @return Flux d'arêtes rapides
         */
        @Query("""
                        SELECT * FROM road_edges
                        WHERE max_speed_kmh >= :minSpeed
                        """)
        Flux<RoadEdge> findFastEdges(@Param("minSpeed") Integer minSpeed);

        /**
         * Recherche d'arêtes par nom de rue.
         * 
         * @param streetName Nom de rue (partiel ou complet)
         * @return Flux d'arêtes correspondantes
         */
        @Query("""
                        SELECT * FROM road_edges
                        WHERE road_name ILIKE CONCAT('%', :streetName, '%')
                        """)
        Flux<RoadEdge> searchByStreetName(@Param("streetName") String streetName);
}