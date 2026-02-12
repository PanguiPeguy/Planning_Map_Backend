-- ================================================================
-- MIGRATION: Charger le réseau routier complet du Cameroun
-- ================================================================
-- Ce script charge toutes les données du fichier 02_cameroon_road_network.sql
-- et calcule automatiquement distance_meters à partir de distance_km
-- ================================================================

\echo '🔄 Chargement du réseau routier complet du Cameroun...'

-- Charger les données depuis le fichier SQL
\i /docker-entrypoint-initdb.d/02_cameroon_road_network.sql

-- Calculer distance_meters pour toutes les arêtes
UPDATE road_edges 
SET distance_meters = distance_km * 1000 
WHERE distance_meters IS NULL AND distance_km IS NOT NULL;

-- Vérifier les résultats
\echo '✅ Vérification des données chargées:'
SELECT 
    COUNT(*) as total_nodes,
    COUNT(DISTINCT CASE WHEN node_type = 'city' THEN node_id END) as cities,
    COUNT(DISTINCT CASE WHEN node_type = 'junction' THEN node_id END) as junctions
FROM road_nodes;

SELECT 
    COUNT(*) as total_edges,
    COUNT(CASE WHEN distance_meters IS NOT NULL THEN 1 END) as with_distance_meters,
    ROUND(SUM(distance_km)::numeric, 2) as total_network_km
FROM road_edges;

\echo '✅ Migration terminée!'
