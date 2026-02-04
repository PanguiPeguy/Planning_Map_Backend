package com.enspy.tripplanning.routing.repository;

import com.enspy.tripplanning.routing.model.RoadNode;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository réactif pour les nœuds du graphe routier.
 * 
 * Fournit les opérations CRUD et des requêtes spatiales optimisées
 * pour le calcul d'itinéraires.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2024-12-15
 */
@Repository
public interface RoadNodeRepository extends R2dbcRepository<RoadNode, Long> {

        /**
         * Recherche un nœud par son identifiant OSM
         * 
         * @param osmId Identifiant OpenStreetMap
         * @return Nœud correspondant
         */
        Mono<RoadNode> findByOsmId(Long osmId);

        /**
         * Recherche des nœuds par type
         * 
         * @param nodeType Type de nœud ("intersection", "poi_reference", etc.)
         * @return Flux de nœuds du type spécifié
         */
        Flux<RoadNode> findByNodeType(String nodeType);

        /**
         * Recherche des nœuds associés à un POI spécifique
         * 
         * @param poiId ID du Point d'Intérêt
         * @return Flux de nœuds liés au POI
         */
        Flux<RoadNode> findByPoiId(Long poiId);

        /**
         * Trouve le nœud le plus proche d'une position géographique donnée.
         * 
         * Utilise la fonction PostGIS ST_Distance pour calculer la distance
         * entre le point fourni et tous les nœuds.
         * 
         * Cette requête est critique pour :
         * - Snapping d'un POI sur le graphe routier
         * - Trouver le point de départ/arrivée le plus proche
         * 
         * Complexité: O(V) où V = nombre de nœuds
         * Performance: ~10-50ms sur 100k nœuds avec index spatial
         * 
         * @param latitude  Latitude du point
         * @param longitude Longitude du point
         *                  /**
         *                  Trouve le nœud routier le plus proche d'une coordonnée GPS.
         * 
         *                  PERFORMANCE CRITIQUE: Utilise l'opérateur de distance <-> de
         *                  PostGIS
         *                  qui exploite l'index GIST pour une recherche ultra-rapide en
         *                  O(log n).
         * 
         *                  L'opérateur <-> retourne la distance et trie par celle-ci
         *                  automatiquement.
         *                  C'est la méthode la plus rapide pour "snap to nearest node".
         * 
         * @param latitude  Latitude GPS
         * @param longitude Longitude GPS
         * @return Mono du nœud le plus proche
         */
        @Query("""
                        SELECT n.* FROM road_nodes n
                        WHERE EXISTS (SELECT 1 FROM road_edges e WHERE e.source_node_id = n.node_id OR e.target_node_id = n.node_id)
                        ORDER BY n.geom <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
                        LIMIT 1
                        """)
        Mono<RoadNode> findNearestNode(
                        @Param("lat") Double latitude,
                        @Param("lon") Double longitude);

        /**
         * Trouve tous les nœuds dans un rayon donné autour d'un point.
         * 
         * Utilise ST_DWithin pour une recherche spatiale efficace.
         * Le rayon est en mètres.
         * 
         * Cas d'usage:
         * - Trouver tous les nœuds accessibles depuis un POI
         * - Recherche de nœuds dans une zone géographique
         * 
         * @param latitude     Latitude du centre
         * @param longitude    Longitude du centre
         * @param radiusMeters Rayon de recherche en mètres
         * @return Flux de nœuds dans le rayon
         */
        @Query("""
                        SELECT * FROM road_nodes
                        WHERE ST_DWithin(
                            geom,
                            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                            :radiusMeters
                        )
                        ORDER BY ST_Distance(
                            geom,
                            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
                        )
                        """)
        Flux<RoadNode> findNodesWithinRadius(
                        @Param("latitude") Double latitude,
                        @Param("longitude") Double longitude,
                        @Param("radiusMeters") Double radiusMeters);

        /**
         * Trouve les nœuds dans un bounding box (zone rectangulaire).
         * 
         * Très efficace pour charger une portion du graphe en mémoire
         * pour les calculs d'itinéraires locaux.
         * 
         * @param minLat Latitude minimale
         * @param minLon Longitude minimale
         * @param maxLat Latitude maximale
         * @param maxLon Longitude maximale
         * @return Flux de nœuds dans la zone
         */
        @Query("""
                        SELECT node_id, latitude, longitude FROM road_nodes
                        WHERE geom && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
                        """)
        Flux<RoadNode> findNodesInBoundingBox(
                        @Param("minLat") Double minLat,
                        @Param("minLon") Double minLon,
                        @Param("maxLat") Double maxLat,
                        @Param("maxLon") Double maxLon);

        /**
         * Compte le nombre total de nœuds dans le graphe.
         * Utile pour les statistiques et monitoring.
         * 
         * @return Nombre total de nœuds
         */
        @Query("SELECT COUNT(*) FROM road_nodes")
        Mono<Long> countTotalNodes();

        /**
         * Trouve tous les nœuds qui sont des intersections.
         * Les intersections sont des points stratégiques du graphe.
         * 
         * @return Flux d'intersections
         */
        @Query("SELECT * FROM road_nodes WHERE node_type = 'intersection'")
        Flux<RoadNode> findAllIntersections();

        /**
         * Trouve tous les nœuds liés à des POI.
         * Utile pour intégrer les Points d'Intérêt dans les itinéraires.
         * 
         * @return Flux de nœuds-POI
         */
        @Query("SELECT * FROM road_nodes WHERE poi_id IS NOT NULL")
        Flux<RoadNode> findAllPOINodes();

        /**
         * Recherche des nœuds par nom (ILIKE pour insensibilité à la casse).
         * 
         * @param name Nom ou partie du nom
         * @return Flux de nœuds correspondants
         */
        @Query("SELECT * FROM road_nodes WHERE name ILIKE CONCAT('%', :name, '%')")
        Flux<RoadNode> searchByName(@Param("name") String name);
}