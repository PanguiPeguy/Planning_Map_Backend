-- ================================================================
-- RÉSEAU ROUTIER COMPLET DU CAMEROUN
-- ================================================================
-- Ce fichier contient le réseau routier réaliste du Cameroun
-- couvrant les routes nationales N1, N2, N3, N4, N5, N6
-- avec des distances et temps de trajet basés sur Google Maps
-- ================================================================

-- ================================================================
-- SUPPRESSION DES DONNÉES EXISTANTES
-- ================================================================
DELETE FROM road_edges WHERE edge_id > 0;
DELETE FROM road_nodes WHERE node_id > 0;

-- ================================================================
-- NŒUDS ROUTIERS - VILLES PRINCIPALES ET JONCTIONS
-- ================================================================

INSERT INTO road_nodes (node_id, latitude, longitude, name, node_type) VALUES
-- ================================================================
-- RÉGION CENTRE (Yaoundé et environs)
-- ================================================================
(1, 3.8667, 11.5167, 'Yaoundé Centre', 'city'),
(2, 3.8500, 11.4900, 'Yaoundé Melen', 'junction'),
(3, 3.8200, 11.4500, 'Yaoundé Sud', 'junction'),
(4, 3.9100, 11.5300, 'Yaoundé Nord', 'junction'),
(5, 3.5167, 11.3500, 'Mbalmayo', 'city'),
(6, 4.1500, 11.5600, 'Obala', 'city'),

-- ================================================================
-- RÉGION LITTORAL (Douala et environs)
-- ================================================================
(10, 4.0511, 9.7679, 'Douala Centre', 'city'),
(11, 4.0300, 9.7500, 'Douala Bonabéri', 'junction'),
(12, 4.0800, 9.8000, 'Douala PK14', 'junction'),
(13, 3.8000, 10.1300, 'Edéa', 'city'),
(14, 4.9500, 9.9400, 'Nkongsamba', 'city'),

-- ================================================================
-- RÉGION OUEST (Bafoussam et environs)
-- ================================================================
(20, 5.4781, 10.4178, 'Bafoussam', 'city'),
(21, 5.4500, 10.0700, 'Dschang', 'city'),
(22, 5.6300, 10.2500, 'Mbouda', 'city'),
(23, 5.2000, 10.1500, 'Bangangté', 'city'),

-- ================================================================
-- RÉGION NORD-OUEST (Bamenda et environs)
-- ================================================================
(30, 5.9597, 10.1453, 'Bamenda', 'city'),
(31, 6.2000, 10.6700, 'Kumbo', 'city'),
(32, 6.4500, 10.3800, 'Wum', 'city'),

-- ================================================================
-- RÉGION ADAMAOUA (Ngaoundéré)
-- ================================================================
(40, 7.3167, 13.5833, 'Ngaoundéré', 'city'),
(41, 6.4600, 12.2200, 'Tibati', 'city'),
(42, 6.9000, 13.0500, 'Meiganga', 'city'),

-- ================================================================
-- RÉGION NORD (Garoua)
-- ================================================================
(50, 9.3012, 13.3964, 'Garoua', 'city'),
(51, 8.5000, 13.6800, 'Poli', 'city'),
(52, 8.8000, 13.1500, 'Lagdo', 'junction'),

-- ================================================================
-- RÉGION EXTRÊME-NORD (Maroua)
-- ================================================================
(60, 10.5964, 14.3167, 'Maroua', 'city'),
(61, 11.0500, 14.7300, 'Mokolo', 'city'),
(62, 11.4000, 14.2000, 'Kousseri', 'city'),
(63, 10.0500, 14.5500, 'Yagoua', 'city'),

-- ================================================================
-- RÉGION SUD (Ebolowa, Kribi)
-- ================================================================
(70, 2.9000, 11.1500, 'Ebolowa', 'city'),
(71, 2.9378, 9.9078, 'Kribi', 'city'),
(72, 3.1500, 10.7500, 'Sangmélima', 'city'),
(73, 2.6000, 10.0000, 'Campo', 'city'),

-- ================================================================
-- RÉGION EST (Bertoua)
-- ================================================================
(80, 4.5833, 13.6833, 'Bertoua', 'city'),
(81, 4.4300, 14.3500, 'Batouri', 'city'),
(82, 3.8000, 13.1500, 'Abong-Mbang', 'city'),

-- ================================================================
-- RÉGION SUD-OUEST (Buea, Limbé, Kumba)
-- ================================================================
(90, 4.1561, 9.2325, 'Buea', 'city'),
(91, 4.0167, 9.2167, 'Limbé', 'city'),
(92, 4.6333, 9.4500, 'Kumba', 'city'),
(93, 4.2000, 9.3000, 'Tiko', 'junction'),

-- ================================================================
-- JONCTIONS INTERMÉDIAIRES SUR ROUTES NATIONALES
-- ================================================================

-- N3: Yaoundé → Douala (jonctions tous les ~50km)
(100, 3.7500, 11.2000, 'Mbankomo', 'junction'),
(101, 3.6500, 10.9000, 'Matomb', 'junction'),
(102, 3.7000, 10.6000, 'Makak', 'junction'),
(103, 3.8500, 10.3000, 'Ngambé-Tikar', 'junction'),

-- N1: Yaoundé → Bafoussam → Ngaoundéré → Garoua → Maroua
(110, 4.3500, 11.8500, 'Bafia', 'junction'),
(111, 5.0000, 12.3000, 'Batchenga', 'junction'),
(112, 6.0000, 11.5000, 'Foumban', 'junction'),
(113, 6.5000, 12.0000, 'Banyo', 'junction'),

-- N4: Douala → Nkongsamba → Bafoussam → Bamenda
(120, 4.5000, 9.8500, 'Loum', 'junction'),
(121, 5.2000, 10.3000, 'Melong', 'junction'),

-- N2: Yaoundé → Bertoua
(130, 4.0500, 12.3500, 'Ayos', 'junction'),
(131, 4.3000, 13.0000, 'Abong-Mbang Junction', 'junction'),

-- N6: Edéa → Kribi → Ebolowa
(140, 3.4500, 10.0500, 'Bipindi', 'junction'),
(141, 3.2000, 10.5000, 'Lolodorf', 'junction'),

-- N5: Douala → Buea → Limbé
(150, 4.0800, 9.5000, 'Mutengene', 'junction')

ON CONFLICT (node_id) DO UPDATE SET
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    name = EXCLUDED.name,
    node_type = EXCLUDED.node_type;

-- Mettre à jour les géométries des nœuds
UPDATE road_nodes SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) WHERE geom IS NULL;

-- ================================================================
-- ARÊTES ROUTIÈRES - ROUTES NATIONALES
-- ================================================================

INSERT INTO road_edges (source_node_id, target_node_id, road_name, road_type, distance_km, max_speed_kmh, travel_time_seconds, one_way) VALUES

-- ================================================================
-- N3: YAOUNDÉ → DOUALA (245 km, ~3h30)
-- Route principale la plus empruntée
-- ================================================================
(1, 100, 'RN3', 'motorway', 25.0, 90, 1000, false),
(100, 1, 'RN3', 'motorway', 25.0, 90, 1000, false),
(100, 101, 'RN3', 'motorway', 45.0, 90, 1800, false),
(101, 100, 'RN3', 'motorway', 45.0, 90, 1800, false),
(101, 102, 'RN3', 'motorway', 40.0, 90, 1600, false),
(102, 101, 'RN3', 'motorway', 40.0, 90, 1600, false),
(102, 103, 'RN3', 'motorway', 35.0, 90, 1400, false),
(103, 102, 'RN3', 'motorway', 35.0, 90, 1400, false),
(103, 13, 'RN3', 'motorway', 30.0, 80, 1350, false),
(13, 103, 'RN3', 'motorway', 30.0, 80, 1350, false),
(13, 10, 'RN3', 'motorway', 70.0, 90, 2800, false),
(10, 13, 'RN3', 'motorway', 70.0, 90, 2800, false),

-- ================================================================
-- N1: YAOUNDÉ → BAFOUSSAM → NGAOUNDÉRÉ → GAROUA → MAROUA
-- Route la plus longue du Cameroun (~1400 km)
-- ================================================================

-- Yaoundé → Bafoussam (280 km, ~4h30)
(1, 4, 'RN1', 'primary', 8.0, 60, 480, false),
(4, 1, 'RN1', 'primary', 8.0, 60, 480, false),
(4, 110, 'RN1', 'primary', 65.0, 80, 2925, false),
(110, 4, 'RN1', 'primary', 65.0, 80, 2925, false),
(110, 111, 'RN1', 'primary', 85.0, 80, 3825, false),
(111, 110, 'RN1', 'primary', 85.0, 80, 3825, false),
(111, 23, 'RN1', 'primary', 45.0, 70, 2314, false),
(23, 111, 'RN1', 'primary', 45.0, 70, 2314, false),
(23, 20, 'RN1', 'primary', 77.0, 70, 3960, false),
(20, 23, 'RN1', 'primary', 77.0, 70, 3960, false),

-- Bafoussam → Ngaoundéré (340 km, ~5h)
(20, 112, 'RN1', 'primary', 85.0, 70, 4371, false),
(112, 20, 'RN1', 'primary', 85.0, 70, 4371, false),
(112, 113, 'RN1', 'primary', 95.0, 70, 4886, false),
(113, 112, 'RN1', 'primary', 95.0, 70, 4886, false),
(113, 41, 'RN1', 'primary', 80.0, 70, 4114, false),
(41, 113, 'RN1', 'primary', 80.0, 70, 4114, false),
(41, 42, 'RN1', 'primary', 40.0, 70, 2057, false),
(42, 41, 'RN1', 'primary', 40.0, 70, 2057, false),
(42, 40, 'RN1', 'primary', 40.0, 70, 2057, false),
(40, 42, 'RN1', 'primary', 40.0, 70, 2057, false),

-- Ngaoundéré → Garoua (320 km, ~4h30)
(40, 51, 'RN1', 'primary', 165.0, 75, 7920, false),
(51, 40, 'RN1', 'primary', 165.0, 75, 7920, false),
(51, 52, 'RN1', 'primary', 85.0, 75, 4080, false),
(52, 51, 'RN1', 'primary', 85.0, 75, 4080, false),
(52, 50, 'RN1', 'primary', 70.0, 75, 3360, false),
(50, 52, 'RN1', 'primary', 70.0, 75, 3360, false),

-- Garoua → Maroua (285 km, ~4h)
(50, 60, 'RN1', 'primary', 285.0, 75, 13680, false),
(60, 50, 'RN1', 'primary', 285.0, 75, 13680, false),

-- Maroua → Mokolo (65 km, ~1h)
(60, 61, 'RN1', 'primary', 65.0, 70, 3343, false),
(61, 60, 'RN1', 'primary', 65.0, 70, 3343, false),

-- Maroua → Kousseri (140 km, ~2h)
(60, 62, 'RN12', 'primary', 140.0, 70, 7200, false),
(62, 60, 'RN12', 'primary', 140.0, 70, 7200, false),

-- ================================================================
-- N4: DOUALA → NKONGSAMBA → BAFOUSSAM → BAMENDA (370 km, ~5h30)
-- ================================================================
(10, 11, 'RN4', 'primary', 5.0, 50, 360, false),
(11, 10, 'RN4', 'primary', 5.0, 50, 360, false),
(11, 120, 'RN4', 'primary', 55.0, 70, 2829, false),
(120, 11, 'RN4', 'primary', 55.0, 70, 2829, false),
(120, 14, 'RN4', 'primary', 45.0, 70, 2314, false),
(14, 120, 'RN4', 'primary', 45.0, 70, 2314, false),
(14, 121, 'RN4', 'primary', 60.0, 70, 3086, false),
(121, 14, 'RN4', 'primary', 60.0, 70, 3086, false),
(121, 20, 'RN4', 'primary', 55.0, 70, 2829, false),
(20, 121, 'RN4', 'primary', 55.0, 70, 2829, false),
(20, 30, 'RN4', 'primary', 150.0, 65, 8308, false),
(30, 20, 'RN4', 'primary', 150.0, 65, 8308, false),

-- Bamenda → Kumbo (95 km, ~1h45)
(30, 31, 'RN11', 'secondary', 95.0, 60, 5700, false),
(31, 30, 'RN11', 'secondary', 95.0, 60, 5700, false),

-- ================================================================
-- N5: DOUALA → BUEA → LIMBÉ (85 km, ~1h30)
-- ================================================================
(10, 150, 'RN5', 'primary', 30.0, 70, 1543, false),
(150, 10, 'RN5', 'primary', 30.0, 70, 1543, false),
(150, 93, 'RN5', 'primary', 20.0, 70, 1029, false),
(93, 150, 'RN5', 'primary', 20.0, 70, 1029, false),
(93, 90, 'RN5', 'primary', 20.0, 60, 1200, false),
(90, 93, 'RN5', 'primary', 20.0, 60, 1200, false),
(90, 91, 'RN5', 'secondary', 15.0, 50, 1080, false),
(91, 90, 'RN5', 'secondary', 15.0, 50, 1080, false),

-- Douala → Kumba (135 km, ~2h15)
(10, 92, 'RN8', 'primary', 135.0, 65, 7477, false),
(92, 10, 'RN8', 'primary', 135.0, 65, 7477, false),

-- ================================================================
-- N6: EDÉA → KRIBI → EBOLOWA (280 km, ~4h30)
-- ================================================================
(13, 140, 'RN7', 'primary', 85.0, 65, 4708, false),
(140, 13, 'RN7', 'primary', 85.0, 65, 4708, false),
(140, 71, 'RN7', 'primary', 65.0, 65, 3600, false),
(71, 140, 'RN7', 'primary', 65.0, 65, 3600, false),
(71, 73, 'RN7', 'secondary', 45.0, 50, 3240, false),
(73, 71, 'RN7', 'secondary', 45.0, 50, 3240, false),
(71, 141, 'RN9', 'primary', 55.0, 60, 3300, false),
(141, 71, 'RN9', 'primary', 55.0, 60, 3300, false),
(141, 70, 'RN9', 'primary', 30.0, 60, 1800, false),
(70, 141, 'RN9', 'primary', 30.0, 60, 1800, false),

-- Yaoundé → Ebolowa (168 km, ~2h45)
(1, 3, 'RN2', 'primary', 12.0, 60, 720, false),
(3, 1, 'RN2', 'primary', 12.0, 60, 720, false),
(3, 5, 'RN2', 'primary', 48.0, 70, 2469, false),
(5, 3, 'RN2', 'primary', 48.0, 70, 2469, false),
(5, 72, 'RN2', 'primary', 58.0, 70, 2983, false),
(72, 5, 'RN2', 'primary', 58.0, 70, 2983, false),
(72, 70, 'RN2', 'primary', 50.0, 70, 2571, false),
(70, 72, 'RN2', 'primary', 50.0, 70, 2571, false),

-- ================================================================
-- N2: YAOUNDÉ → BERTOUA (350 km, ~5h)
-- ================================================================
(1, 130, 'RN10', 'primary', 95.0, 70, 4886, false),
(130, 1, 'RN10', 'primary', 95.0, 70, 4886, false),
(130, 131, 'RN10', 'primary', 85.0, 70, 4371, false),
(131, 130, 'RN10', 'primary', 85.0, 70, 4371, false),
(131, 82, 'RN10', 'primary', 75.0, 70, 3857, false),
(82, 131, 'RN10', 'primary', 75.0, 70, 3857, false),
(82, 80, 'RN10', 'primary', 95.0, 70, 4886, false),
(80, 82, 'RN10', 'primary', 95.0, 70, 4886, false),

-- Bertoua → Batouri (120 km, ~2h)
(80, 81, 'RN10', 'secondary', 120.0, 60, 7200, false),
(81, 80, 'RN10', 'secondary', 120.0, 60, 7200, false),

-- ================================================================
-- ROUTES RÉGIONALES COMPLÉMENTAIRES
-- ================================================================

-- Yaoundé → Obala (40 km, ~45min)
(1, 6, 'Route Régionale', 'secondary', 40.0, 60, 2400, false),
(6, 1, 'Route Régionale', 'secondary', 40.0, 60, 2400, false),

-- Bafoussam → Dschang (55 km, ~1h)
(20, 21, 'Route Régionale', 'secondary', 55.0, 60, 3300, false),
(21, 20, 'Route Régionale', 'secondary', 55.0, 60, 3300, false),

-- Bafoussam → Mbouda (40 km, ~45min)
(20, 22, 'Route Régionale', 'secondary', 40.0, 60, 2400, false),
(22, 20, 'Route Régionale', 'secondary', 40.0, 60, 2400, false),

-- Douala interne
(10, 12, 'Boulevard de la Liberté', 'primary', 8.0, 50, 576, false),
(12, 10, 'Boulevard de la Liberté', 'primary', 8.0, 50, 576, false)

ON CONFLICT DO NOTHING;

-- Mettre à jour les géométries des arêtes
UPDATE road_edges e
SET geom = ST_MakeLine(s.geom, t.geom)
FROM road_nodes s, road_nodes t
WHERE e.source_node_id = s.node_id 
AND e.target_node_id = t.node_id 
AND e.geom IS NULL;

-- ================================================================
-- STATISTIQUES DU RÉSEAU ROUTIER
-- ================================================================
DO $$
DECLARE
    v_nodes INT;
    v_edges INT;
    v_total_km DECIMAL;
BEGIN
    SELECT COUNT(*) INTO v_nodes FROM road_nodes;
    SELECT COUNT(*) INTO v_edges FROM road_edges;
    SELECT SUM(distance_km) INTO v_total_km FROM road_edges WHERE one_way = false OR source_node_id < target_node_id;
    
    RAISE NOTICE '========================================';
    RAISE NOTICE 'RÉSEAU ROUTIER CAMEROUN INITIALISÉ';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Nœuds routiers: %', v_nodes;
    RAISE NOTICE 'Arêtes routières: %', v_edges;
    RAISE NOTICE 'Distance totale: % km', ROUND(v_total_km, 0);
    RAISE NOTICE '========================================';
END $$;
