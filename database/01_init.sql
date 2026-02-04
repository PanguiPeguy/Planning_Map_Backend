-- ================================================================
-- SCRIPT D'INITIALISATION COMPLET DE LA BASE DE DONNÉES
-- Système de Planification d'Itinéraires avec POI
-- Combine tous les scripts en un seul fichier cohérent
-- ================================================================

-- Activer les extensions nécessaires
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ================================================================
-- 1. SUPPRIMER LES TABLES EXISTANTES (ATTENTION: Perte de données)
-- ================================================================
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS trip_members CASCADE;
DROP TABLE IF EXISTS trip_waypoints CASCADE;
DROP TABLE IF EXISTS trips CASCADE;
DROP TABLE IF EXISTS poi_favorites CASCADE;
DROP TABLE IF EXISTS poi_likes CASCADE;
DROP TABLE IF EXISTS poi_reviews CASCADE;
DROP TABLE IF EXISTS pois CASCADE;
DROP TABLE IF EXISTS poi_categories CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS plannings CASCADE;
DROP TABLE IF EXISTS planning_items CASCADE;
DROP TABLE IF EXISTS itineraries CASCADE;

-- ================================================================
-- 2. TABLE: users (Utilisateurs)
-- ================================================================
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Profil
    company_name VARCHAR(100),
    phone VARCHAR(20),
    city VARCHAR(100),
    transportmode VARCHAR(50),
    profile_photo_url VARCHAR(500),
    
    -- Email verification & Password reset
    is_verified BOOLEAN DEFAULT FALSE,
    verification_token VARCHAR(100),
    reset_password_token VARCHAR(100),
    reset_password_expires TIMESTAMP,
    
    -- Login stats
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(50),
    login_count INT DEFAULT 0
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- ================================================================
-- 3. TABLE: poi_categories (Catégories de POI) - VERSION CORRIGÉE
-- ================================================================
CREATE TABLE poi_categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    name_en VARCHAR(100),  -- Colonne supplémentaire
    slug VARCHAR(100) UNIQUE,  -- Colonne supplémentaire
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(7),
    parent_category_id BIGINT REFERENCES poi_categories(category_id) ON DELETE SET NULL,
    order_index INT DEFAULT 0,  -- Colonne supplémentaire
    is_active BOOLEAN DEFAULT TRUE,  -- Colonne supplémentaire
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- 4. TABLE: pois (Points d'Intérêt)
-- ================================================================
CREATE TABLE pois (
    poi_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    latitude DECIMAL(10, 8) NOT NULL CHECK (latitude >= -90 AND latitude <= 90),
    longitude DECIMAL(11, 8) NOT NULL CHECK (longitude >= -180 AND longitude <= 180),
    category_id BIGINT NOT NULL REFERENCES poi_categories(category_id) ON DELETE CASCADE,
    
    -- Adresse structurée
    address_street VARCHAR(255),
    address_city VARCHAR(100),
    address_postal_code VARCHAR(20),
    address_region VARCHAR(100),
    address_neighborhood VARCHAR(100),
    address_country VARCHAR(100) DEFAULT 'Cameroun',
    
    -- Contact
    phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(255),
    
    -- Informations complémentaires
    type VARCHAR(50),
    rating DECIMAL(3, 2) DEFAULT 0.0 CHECK (rating >= 0 AND rating <= 5),
    review_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    opening_hours JSONB,
    services JSONB,
    amenities JSONB,
    price_level INT CHECK (price_level >= 1 AND price_level <= 4),
    price_range VARCHAR(50),
    tags JSONB,
    
    -- Image
    image_url VARCHAR(500),
    images JSONB,

    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE, 
    verified_at TIMESTAMP,
    
    -- Métadonnées
    metadata JSONB,
    
    -- Géométrie PostGIS
    geom GEOMETRY(Point, 4326),
    
    -- Audit
    created_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes POI
CREATE INDEX idx_pois_latitude ON pois(latitude);
CREATE INDEX idx_pois_longitude ON pois(longitude);
CREATE INDEX idx_pois_location ON pois(latitude, longitude);
CREATE INDEX idx_pois_category_id ON pois(category_id);
CREATE INDEX idx_pois_geom ON pois USING GIST(geom);
CREATE INDEX idx_pois_name_trgm ON pois USING gin(name gin_trgm_ops);
CREATE INDEX idx_pois_city ON pois(address_city);
CREATE INDEX idx_pois_rating ON pois(rating DESC);

-- ================================================================
-- 5. TABLE: poi_reviews (Avis sur POI)
-- ================================================================
CREATE TABLE poi_reviews (
    review_id BIGSERIAL PRIMARY KEY,
    poi_id BIGINT NOT NULL REFERENCES pois(poi_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rating DECIMAL(2, 1) NOT NULL CHECK (rating >= 0 AND rating <= 5),
    comment TEXT,
    images TEXT[],
    is_verified_visit BOOLEAN DEFAULT FALSE,
    is_moderated BOOLEAN DEFAULT FALSE,
    report_count INT DEFAULT 0,
    helpful_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(poi_id, user_id)
);

CREATE INDEX idx_reviews_poi_id ON poi_reviews(poi_id);
CREATE INDEX idx_reviews_user_id ON poi_reviews(user_id);
CREATE INDEX idx_reviews_rating ON poi_reviews(rating DESC);
CREATE INDEX idx_reviews_created_at ON poi_reviews(created_at DESC);

-- ================================================================
-- 5b. TABLE: poi_likes (Likes sur POI)
-- ================================================================
CREATE TABLE poi_likes (
    like_id BIGSERIAL PRIMARY KEY,
    poi_id BIGINT NOT NULL REFERENCES pois(poi_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(poi_id, user_id)
);

CREATE INDEX idx_poi_likes_poi_id ON poi_likes(poi_id);
CREATE INDEX idx_poi_likes_user_id ON poi_likes(user_id);

-- ================================================================
-- 5c. TABLE: poi_favorites (Favoris POI)
-- ================================================================
CREATE TABLE poi_favorites (
    favorite_id BIGSERIAL PRIMARY KEY,
    poi_id BIGINT NOT NULL REFERENCES pois(poi_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    UNIQUE(poi_id, user_id)
);

CREATE INDEX idx_poi_favorites_poi_id ON poi_favorites(poi_id);
CREATE INDEX idx_poi_favorites_user_id ON poi_favorites(user_id);

-- ================================================================
-- 6. TABLE: trips (Voyages)
-- ================================================================
CREATE TABLE trips (
    trip_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    
    -- Dates
    start_date DATE,
    end_date DATE,
    
    -- Statut
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    
    -- Paramètres
    is_public BOOLEAN DEFAULT FALSE,
    is_collaborative BOOLEAN DEFAULT FALSE,
    share_token VARCHAR(50) UNIQUE,
    
    -- Statistiques calculées
    total_distance_km DECIMAL(10, 2) DEFAULT 0,
    total_duration_minutes INT DEFAULT 0,
    estimated_cost DECIMAL(10, 2) DEFAULT 0,
    waypoint_count INT DEFAULT 0,
    
    -- Métadonnées
    metadata JSONB,
    
    -- Propriétaire
    owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_trips_owner_id ON trips(owner_id);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_share_token ON trips(share_token) WHERE share_token IS NOT NULL;
CREATE INDEX idx_trips_start_date ON trips(start_date);
CREATE INDEX idx_trips_is_public ON trips(is_public) WHERE is_public = TRUE;

-- ================================================================
-- 7. TABLE: trip_waypoints (Étapes de voyage)
-- ================================================================
CREATE TABLE trip_waypoints (
    waypoint_id BIGSERIAL PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(trip_id) ON DELETE CASCADE,
    
    -- Ordre dans le voyage
    order_index INT NOT NULL,
    
    -- Type d'étape
    waypoint_type VARCHAR(20) DEFAULT 'WAYPOINT' CHECK (waypoint_type IN ('START', 'WAYPOINT', 'END')),
    
    -- Référence POI (optionnel)
    poi_id BIGINT REFERENCES pois(poi_id) ON DELETE SET NULL,
    
    -- OU point custom
    custom_name VARCHAR(255),
    custom_latitude DECIMAL(10, 8) CHECK (custom_latitude >= -90 AND custom_latitude <= 90),
    custom_longitude DECIMAL(11, 8) CHECK (custom_longitude >= -180 AND custom_longitude <= 180),
    
    -- Timing prévu
    planned_arrival_time TIMESTAMP,
    planned_departure_time TIMESTAMP,
    planned_duration_minutes INT,
    
    -- Timing réel
    actual_arrival_time TIMESTAMP,
    actual_departure_time TIMESTAMP,
    
    -- Notes
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Contraintes
    CHECK (
        (poi_id IS NOT NULL) OR 
        (custom_name IS NOT NULL AND custom_latitude IS NOT NULL AND custom_longitude IS NOT NULL)
    ),
    UNIQUE(trip_id, order_index)
);

CREATE INDEX idx_waypoints_trip_id ON trip_waypoints(trip_id);
CREATE INDEX idx_waypoints_poi_id ON trip_waypoints(poi_id) WHERE poi_id IS NOT NULL;
CREATE INDEX idx_waypoints_order ON trip_waypoints(trip_id, order_index);

-- ================================================================
-- 8. TABLE: trip_members (Membres collaborateurs)
-- ================================================================
CREATE TABLE trip_members (
    trip_id UUID NOT NULL REFERENCES trips(trip_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Rôle
    role VARCHAR(20) DEFAULT 'VIEWER' CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    
    -- Paramètres
    notifications_enabled BOOLEAN DEFAULT TRUE,
    
    -- Timestamps
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP,
    
    PRIMARY KEY (trip_id, user_id)
);

CREATE INDEX idx_trip_members_user_id ON trip_members(user_id);
CREATE INDEX idx_trip_members_role ON trip_members(role);

-- ================================================================
-- 9. TABLE: notifications (Notifications utilisateur)
-- ================================================================
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Type de notification
    type VARCHAR(50) NOT NULL CHECK (type IN (
        'TRIP_INVITE', 'TRIP_UPDATE', 'WAYPOINT_ADDED', 'MEMBER_JOINED',
        'POI_VERIFIED', 'REVIEW_REPLY', 'SYSTEM_ALERT', 'NEW_POI', 'NEW_USER',
        'USER_DELETED', 'POI_COMMENT', 'POI_EDITED', 'POI_DELETED', 'WELCOME', 'FAVORITE_ACTIVITY'
    )),
    
    -- Contenu
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    
    -- Entité associée
    related_entity_type VARCHAR(50),
    related_entity_id VARCHAR(50),
    action_url VARCHAR(255),
    
    -- État
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- ================================================================
-- 10. TABLE: road_nodes (Nœuds du graphe routier)
-- ================================================================
CREATE TABLE IF NOT EXISTS road_nodes (
    node_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    geom GEOMETRY(Point, 4326),
    
    -- Métadonnées OSM
    osm_id BIGINT,
    node_type VARCHAR(50),
    tags JSONB,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_road_nodes_geom ON road_nodes USING GIST(geom);
CREATE INDEX idx_road_nodes_location ON road_nodes(latitude, longitude);
CREATE UNIQUE INDEX idx_road_nodes_osm_id ON road_nodes(osm_id) WHERE osm_id IS NOT NULL;

-- ================================================================
-- 11. TABLE: road_edges (Arêtes du graphe routier)
-- ================================================================
CREATE TABLE IF NOT EXISTS road_edges (
    edge_id BIGSERIAL PRIMARY KEY,
    source_node_id BIGINT NOT NULL REFERENCES road_nodes(node_id) ON DELETE CASCADE,
    target_node_id BIGINT NOT NULL REFERENCES road_nodes(node_id) ON DELETE CASCADE,
    
    -- Propriétés de la route
    distance_km DECIMAL(10, 3) NOT NULL,
    distance_meters DOUBLE PRECISION,
    max_speed_kmh INT,
    road_type VARCHAR(50),
    road_name VARCHAR(255),
    one_way BOOLEAN DEFAULT FALSE,
    
    -- Coûts pour A*
    travel_time_seconds INT,
    
    -- Métadonnées OSM
    osm_way_id BIGINT,
    tags JSONB,
    
    -- Géométrie de la route
    geom GEOMETRY(LineString, 4326),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_road_edges_source ON road_edges(source_node_id);
CREATE INDEX idx_road_edges_target ON road_edges(target_node_id);
CREATE INDEX idx_road_edges_geom ON road_edges USING GIST(geom);
CREATE INDEX idx_road_edges_road_type ON road_edges(road_type);

-- ================================================================
-- 12. TABLE: itineraries (V3 - Itinéraires)
-- ================================================================
CREATE TABLE itineraries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    origin_location VARCHAR(255),
    destination_location VARCHAR(255),
    waypoints_json TEXT,
    geometry_encoded TEXT,
    distance_meters DOUBLE PRECISION,
    duration_seconds INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- 13. TABLE: plannings (V2 - Planifications)
-- ================================================================
CREATE TABLE plannings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_planning_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ================================================================
-- 14. TABLE: planning_items (V2 - Éléments de planification)
-- ================================================================
CREATE TABLE planning_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    planning_id UUID NOT NULL,
    
    -- Informations d'origine
    origin_city VARCHAR(255) NOT NULL,
    origin_latitude DECIMAL(10, 8),  -- AJOUTER CETTE LIGNE
    origin_longitude DECIMAL(11, 8), -- AJOUTER CETTE LIGNE
    
    -- Informations de destination
    destination_city VARCHAR(255) NOT NULL,
    destination_latitude DECIMAL(10, 8),  -- AJOUTER CETTE LIGNE
    destination_longitude DECIMAL(11, 8), -- AJOUTER CETTE LIGNE
    
    planned_date DATE NOT NULL,
    departure_time TIME NOT NULL,
    distance_meters DOUBLE PRECISION,
    travel_time_seconds INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    route_geom TEXT, -- Changed to TEXT to store encoded polyline (consistent with Java entity)
    
    -- Colonnes V3
    optimal_route_geom TEXT,
    itinerary_id UUID,
    selected_waypoints_json TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_planning FOREIGN KEY (planning_id) REFERENCES plannings(id) ON DELETE CASCADE,
    CONSTRAINT fk_planning_items_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE SET NULL
);


-- Indexes pour les nouvelles tables
CREATE INDEX IF NOT EXISTS idx_plannings_user_id ON plannings(user_id);
CREATE INDEX IF NOT EXISTS idx_planning_items_planning_id ON planning_items(planning_id);
CREATE INDEX IF NOT EXISTS idx_itineraries_user_id ON itineraries(user_id);

-- ================================================================
-- TRIGGERS: Mise à jour automatique updated_at
-- ================================================================

-- Fonction générique pour mettre à jour updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers pour toutes les tables avec updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_poi_categories_updated_at BEFORE UPDATE ON poi_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pois_updated_at BEFORE UPDATE ON pois
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_poi_reviews_updated_at BEFORE UPDATE ON poi_reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_trips_updated_at BEFORE UPDATE ON trips
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_trip_waypoints_updated_at BEFORE UPDATE ON trip_waypoints
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_plannings_updated_at BEFORE UPDATE ON plannings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_planning_items_updated_at BEFORE UPDATE ON planning_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_itineraries_updated_at BEFORE UPDATE ON itineraries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ================================================================
-- TRIGGERS: Géométrie PostGIS automatique
-- ================================================================

-- Trigger pour pois
CREATE OR REPLACE FUNCTION update_poi_geom()
RETURNS TRIGGER AS $$
BEGIN
    NEW.geom = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_poi_geom_trigger
    BEFORE INSERT OR UPDATE OF latitude, longitude ON pois
    FOR EACH ROW EXECUTE FUNCTION update_poi_geom();

-- Trigger pour road_nodes
CREATE OR REPLACE FUNCTION update_road_node_geom()
RETURNS TRIGGER AS $$
BEGIN
    NEW.geom = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_road_node_geom_trigger
    BEFORE INSERT OR UPDATE OF latitude, longitude ON road_nodes
    FOR EACH ROW EXECUTE FUNCTION update_road_node_geom();

-- ================================================================
-- TRIGGERS: Statistiques automatiques
-- ================================================================

-- Recalculer rating moyen POI
CREATE OR REPLACE FUNCTION update_poi_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE pois
    SET 
        rating = (SELECT COALESCE(AVG(rating), 0) FROM poi_reviews WHERE poi_id = COALESCE(NEW.poi_id, OLD.poi_id)),
        review_count = (SELECT COUNT(*) FROM poi_reviews WHERE poi_id = COALESCE(NEW.poi_id, OLD.poi_id))
    WHERE poi_id = COALESCE(NEW.poi_id, OLD.poi_id);
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_poi_rating_on_review_insert
    AFTER INSERT ON poi_reviews
    FOR EACH ROW EXECUTE FUNCTION update_poi_rating();

CREATE TRIGGER update_poi_rating_on_review_update
    AFTER UPDATE ON poi_reviews
    FOR EACH ROW EXECUTE FUNCTION update_poi_rating();

CREATE TRIGGER update_poi_rating_on_review_delete
    AFTER DELETE ON poi_reviews
    FOR EACH ROW EXECUTE FUNCTION update_poi_rating();

-- Compter waypoints dans trip
CREATE OR REPLACE FUNCTION update_trip_waypoint_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE trips
    SET waypoint_count = (SELECT COUNT(*) FROM trip_waypoints WHERE trip_id = COALESCE(NEW.trip_id, OLD.trip_id))
    WHERE trip_id = COALESCE(NEW.trip_id, OLD.trip_id);
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_trip_waypoint_count_insert
    AFTER INSERT ON trip_waypoints
    FOR EACH ROW EXECUTE FUNCTION update_trip_waypoint_count();

CREATE TRIGGER update_trip_waypoint_count_delete
    AFTER DELETE ON trip_waypoints
    FOR EACH ROW EXECUTE FUNCTION update_trip_waypoint_count();

-- ================================================================
-- TRIGGERS: Likes & Favoris automatiques
-- ================================================================

-- Mise à jour like_count
CREATE OR REPLACE FUNCTION update_poi_like_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE pois
    SET like_count = (SELECT COUNT(*) FROM poi_likes WHERE poi_id = COALESCE(NEW.poi_id, OLD.poi_id))
    WHERE poi_id = COALESCE(NEW.poi_id, OLD.poi_id);
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_poi_like_count_on_insert
    AFTER INSERT ON poi_likes
    FOR EACH ROW EXECUTE FUNCTION update_poi_like_count();

CREATE TRIGGER update_poi_like_count_on_delete
    AFTER DELETE ON poi_likes
    FOR EACH ROW EXECUTE FUNCTION update_poi_like_count();

-- Mise à jour favorite_count
CREATE OR REPLACE FUNCTION update_poi_favorite_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE pois
    SET favorite_count = (SELECT COUNT(*) FROM poi_favorites WHERE poi_id = COALESCE(NEW.poi_id, OLD.poi_id))
    WHERE poi_id = COALESCE(NEW.poi_id, OLD.poi_id);
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_poi_favorite_count_on_insert
    AFTER INSERT ON poi_favorites
    FOR EACH ROW EXECUTE FUNCTION update_poi_favorite_count();

CREATE TRIGGER update_poi_favorite_count_on_delete
    AFTER DELETE ON poi_favorites
    FOR EACH ROW EXECUTE FUNCTION update_poi_favorite_count();

-- ================================================================
-- TRIGGERS: Génération token partage automatique
-- ================================================================

CREATE OR REPLACE FUNCTION generate_share_token()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_public = TRUE AND NEW.share_token IS NULL THEN
        -- Utiliser uuid_generate_v4() au lieu de gen_random_bytes()
        NEW.share_token = replace(uuid_generate_v4()::text, '-', '');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER generate_share_token_trigger
    BEFORE INSERT OR UPDATE OF is_public ON trips
    FOR EACH ROW EXECUTE FUNCTION generate_share_token();

-- ================================================================
-- FONCTIONS UTILITAIRES
-- ================================================================

-- Calcul distance Haversine
CREATE OR REPLACE FUNCTION calculate_distance(
    lat1 DECIMAL, lon1 DECIMAL,
    lat2 DECIMAL, lon2 DECIMAL
) RETURNS DECIMAL AS $$
DECLARE
    earth_radius DECIMAL := 6371;
BEGIN
    RETURN earth_radius * ACOS(
        COS(RADIANS(lat1)) * COS(RADIANS(lat2)) *
        COS(RADIANS(lon2) - RADIANS(lon1)) +
        SIN(RADIANS(lat1)) * SIN(RADIANS(lat2))
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- POI dans rayon
CREATE OR REPLACE FUNCTION pois_within_radius(
    center_lat DECIMAL,
    center_lon DECIMAL,
    radius_km DECIMAL
) RETURNS TABLE (
    poi_id BIGINT,
    name VARCHAR,
    distance_km DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.poi_id,
        p.name,
        calculate_distance(center_lat, center_lon, p.latitude, p.longitude) AS distance_km
    FROM pois p
    WHERE calculate_distance(center_lat, center_lon, p.latitude, p.longitude) <= radius_km
    ORDER BY distance_km;
END;
$$ LANGUAGE plpgsql;

-- ================================================================
-- VUES MATÉRIALISÉES (Performances)
-- ================================================================

CREATE MATERIALIZED VIEW mv_popular_pois AS
SELECT 
    p.poi_id,
    p.name,
    p.latitude,
    p.longitude,
    p.rating,
    p.review_count,
    c.name AS category_name,
    COUNT(DISTINCT tw.trip_id) AS trip_usage_count
FROM pois p
JOIN poi_categories c ON p.category_id = c.category_id
LEFT JOIN trip_waypoints tw ON tw.poi_id = p.poi_id
GROUP BY p.poi_id, p.name, p.latitude, p.longitude, p.rating, p.review_count, c.name
HAVING p.review_count > 0 OR COUNT(DISTINCT tw.trip_id) > 0
ORDER BY p.rating DESC, p.review_count DESC;

CREATE UNIQUE INDEX idx_mv_popular_pois ON mv_popular_pois(poi_id);

-- ================================================================
-- DONNÉES INITIALES
-- ================================================================

-- 1. UTILISATEURS ADMIN ET USER TEST
-- Password pour admin et user: password123
INSERT INTO users (user_id, username, email, password_hash, role, company_name, phone, city, transportmode, is_verified) VALUES
('22222222-2222-2222-2222-222222222222', 'admin', 'admin@planningmap.cm', 
 '$2a$12$vNUXkZKrZiMysw/GZ.EhkuHphTmTnIloGKFztuaS0ojVTpy6jVrL6', 'ADMIN', 
 'Planning Map Corp', '+237600000000', 'Yaoundé', 'CAR', TRUE),
('11111111-1111-1111-1111-111111111111', 'usertest', 'user@planningmap.cm', 
 '$2a$12$vNUXkZKrZiMysw/GZ.EhkuHphTmTnIloGKFztuaS0ojVTpy6jVrL6', 'USER', 
 NULL, '+237699112233', 'Douala', 'WALK', TRUE)
ON CONFLICT (email) DO NOTHING;

-- 2. CATÉGORIES POI
INSERT INTO poi_categories (name, name_en, slug, description, icon, color, order_index) VALUES
('Hébergement', 'Accommodation', 'accommodation', 'Hôtels, motels, auberges, campings', 'hotel', '#3498DB', 1),
('Restaurant', 'Restaurant', 'restaurant', 'Restaurants, cafés, bars, fast-food', 'restaurant', '#E67E22', 2),
('Péage', 'Toll', 'toll', 'Barrages de péage routiers', 'toll', '#95A5A6', 3),
('Station-service', 'Gas Station', 'gas-station', 'Stations-essence et services automobiles', 'local_gas_station', '#E74C3C', 4),
('Attraction touristique', 'Tourist Attraction', 'tourist-attraction', 'Sites touristiques, monuments, musées', 'attractions', '#9B59B6', 5),
('Hôpital', 'Hospital', 'hospital', 'Centres médicaux, hôpitaux, cliniques', 'local_hospital', '#E67E22', 6),
('Parking', 'Parking', 'parking', 'Aires de stationnement', 'local_parking', '#7F8C8D', 7),
('Aire de repos', 'Rest Area', 'rest-area', 'Aires de repos, haltes routières', 'local_cafe', '#16A085', 8),
('Gare', 'Train Station', 'train-station', 'Gares ferroviaires et routières', 'train', '#34495E', 9),
('Banque', 'Bank', 'bank', 'Banques et distributeurs ATM', 'account_balance', '#27AE60', 10)
ON CONFLICT (name) DO NOTHING;

-- 3. POI À YAOUNDÉ
INSERT INTO pois (poi_id, name, description, latitude, longitude, category_id, address_street, address_city, phone, rating, price_level, services) VALUES
(1, 'Hotel Hilton Yaoundé', 'Hôtel 5 étoiles au centre-ville avec vue panoramique', 3.8667, 11.5167, 
 (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Boulevard du 20 Mai', 'Yaoundé', '+237222123456', 4.8, 4, 
  to_jsonb(ARRAY['wifi', 'parking', 'piscine', 'restaurant'])),
(2, 'Restaurant Le Biniou', 'Restaurant français haut de gamme', 3.8667, 11.5167, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Avenue Kennedy, Bastos', 'Yaoundé', '+237677123456', 4.5, 3, 
  to_jsonb(ARRAY['wifi', 'parking', 'terrasse'])),
(3, 'Total Nlongkak', 'Station service 24/7', 3.8900, 11.5200, 
 (SELECT category_id FROM poi_categories WHERE name = 'Station-service'), 'Nlongkak', 'Yaoundé', '+237699000111', 4.0, 1, 
  to_jsonb(ARRAY['boutique', 'lavage'])),
(4, 'Centre Hospitalier Essos', 'Hôpital général avec urgences', 3.8450, 11.5250, 
 (SELECT category_id FROM poi_categories WHERE name = 'Hôpital'), 'Essos', 'Yaoundé', '+237222234567', 4.2, 2, 
  to_jsonb(ARRAY['urgences', 'parking']))
ON CONFLICT (poi_id) DO NOTHING;

-- 4. POI À DOUALA
INSERT INTO pois (poi_id, name, description, latitude, longitude, category_id, address_street, address_city, phone, rating, price_level, services) VALUES
(11, 'Le Wouri Restaurant', 'Restaurant avec vue sur le fleuve Wouri', 4.0511, 9.7679, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Bonanjo', 'Douala', '+237677345678', 4.6, 3, 
  to_jsonb(ARRAY['wifi', 'parking', 'terrasse', 'vue_mer'])),
(12, 'Hôtel Pullman Douala', 'Hôtel international 5 étoiles', 4.0480, 9.7700, 
 (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Boulevard de la Liberté', 'Douala', '+237233123456', 4.9, 4, 
  to_jsonb(ARRAY['wifi', 'parking', 'piscine', 'spa', 'restaurant']))
ON CONFLICT (poi_id) DO NOTHING;

-- ================================================================
-- AJOUT DES POI MANQUANTS
-- ================================================================

-- POI manquants pour Yaoundé
INSERT INTO pois (poi_id, name, description, latitude, longitude, category_id, address_city, rating) VALUES
(7, 'Total Bastos', 'Station service Total à Bastos', 3.8680, 11.5200, 
 (SELECT category_id FROM poi_categories WHERE name = 'Station-service'), 'Yaoundé', 4.0),
(15, 'Total Akwa', 'Station service Total à Akwa Douala', 4.0520, 9.7690, 
 (SELECT category_id FROM poi_categories WHERE name = 'Station-service'), 'Douala', 4.1),
(41, 'Hôtel Ilomba Kribi', 'Resort de plage à Kribi', 2.9400, 9.9100, 
 (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Kribi', 4.5),
(42, 'Restaurant Les Chutes', 'Restaurant avec vue sur les chutes', 2.9500, 9.9200, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Kribi', 4.3),
(43, 'Chutes de la Lobé', 'Chutes d''eau spectaculaires', 2.9350, 9.9050, 
 (SELECT category_id FROM poi_categories WHERE name = 'Attraction touristique'), 'Kribi', 4.7),
(44, 'Seme Beach Hotel', 'Hôtel de plage à Limbé', 4.0200, 9.2200, 
 (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Limbé', 4.2),
(45, 'Jardin Botanique de Limbé', 'Jardin botanique historique', 4.0180, 9.2150, 
 (SELECT category_id FROM poi_categories WHERE name = 'Attraction touristique'), 'Limbé', 4.4),
(26, 'Talotel Bafoussam', 'Hôtel confortable à Bafoussam', 5.4800, 10.4200, 
 (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Bafoussam', 4.0),
(27, 'Le Palanka', 'Restaurant réputé de Bafoussam', 5.4780, 10.4180, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Bafoussam', 4.2),
(29, 'Ribadou Hotel Garoua', 'Hôtel principal de Garoua', 9.3020, 13.3980, 
 (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Garoua', 4.1),
(30, 'Le Sahel Restaurant', 'Restaurant spécialités sahéliennes', 9.3010, 13.3970, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Garoua', 4.3),
(31, 'Ayaba Hotel Bamenda', 'Hôtel réputé de Bamenda', 5.9600, 10.1460, 
 (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Bamenda', 4.2),
(32, 'Dreamland Restaurant', 'Restaurant populaire à Bamenda', 5.9590, 10.1450, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Bamenda', 4.0),
(4, 'Restaurant La Terrasse', 'Restaurant gastronomique à Yaoundé', 3.8670, 11.5170, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Yaoundé', 4.5),
(5, 'Chez Wou Restaurant', 'Cuisine locale camerounaise', 3.8650, 11.5150, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Yaoundé', 4.3),
(6, 'Pizza Napoli', 'Restaurant italien à Yaoundé', 3.8690, 11.5190, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Yaoundé', 4.2),
(13, 'Le Biniou Douala', 'Restaurant gastronomique français', 4.0520, 9.7680, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Douala', 4.6),
(14, 'Poissonnerie du Port', 'Spécialités fruits de mer', 4.0550, 9.7700, 
 (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Douala', 4.4),
(18, 'Cathédrale de Douala', 'Monument historique et religieux', 4.0500, 9.7650, 
 (SELECT category_id FROM poi_categories WHERE name = 'Attraction touristique'), 'Douala', 4.5),
(19, 'Marché des Fleurs', 'Marché artisanal et produits locaux', 4.0480, 9.7620, 
 (SELECT category_id FROM poi_categories WHERE name = 'Attraction touristique'), 'Douala', 4.0)
ON CONFLICT (poi_id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    category_id = EXCLUDED.category_id;


-- Mettre à jour la géométrie PostGIS pour les nouveaux POI
UPDATE pois SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) 
WHERE geom IS NULL;

-- ================================================================
-- 15. GRAPH ROUTIER CAMEROUN (DONNÉES INITIALES)
-- ================================================================

-- Nœuds (Villes principales et carrefours stratégiques)
INSERT INTO road_nodes (node_id, name, latitude, longitude, node_type) VALUES
-- Littoral
(1, 'Douala (Akwa)', 4.0511, 9.7679, 'CITY'),
(2, 'Douala (Sortie Est)', 4.0100, 9.7900, 'JUNCTION'),
(3, 'Edea', 3.8000, 10.1333, 'CITY'),
(4, 'Pouma', 3.8500, 10.5167, 'CITY'),
(5, 'Boumnyebel', 3.9000, 10.7500, 'JUNCTION'),
-- Centre
(6, 'Yaoundé (Centre)', 3.8480, 11.5021, 'CITY'),
(7, 'Yaoundé (Sortie Nord)', 3.9000, 11.5100, 'JUNCTION'),
(8, 'Obala', 4.1667, 11.5333, 'CITY'),
-- Ouest
(9, 'Bafia', 4.7500, 11.2333, 'CITY'),
(10, 'Makenene', 4.8833, 10.8833, 'CITY'),
(11, 'Bafoussam', 5.4778, 10.4167, 'CITY'),
(12, 'Dschang', 5.4500, 10.0667, 'CITY'),
(13, 'Bafang', 5.1667, 10.1833, 'CITY'),
(14, 'Nkongsamba', 4.9667, 9.9333, 'CITY'),
(15, 'Loum', 4.7167, 9.7333, 'CITY'),
-- Sud/Sud-Ouest
(16, 'Kribi', 2.9333, 9.9167, 'CITY'),
(17, 'Limbé', 4.0244, 9.2032, 'CITY'),
(18, 'Buea', 4.1550, 9.2314, 'CITY'),
(19, 'Tiko', 4.0750, 9.3600, 'CITY'),
(20, 'Mutengene', 4.1000, 9.3000, 'JUNCTION'),
-- Nord
(21, 'Garoua', 9.3000, 13.4000, 'CITY'),
(22, 'Maroua', 10.5930, 14.3200, 'CITY'),
(23, 'Ngaoundéré', 7.3167, 13.5833, 'CITY'),
(24, 'Bertoua', 4.5833, 13.6833, 'CITY')
ON CONFLICT (node_id) DO NOTHING;

-- Reset sequence manually just in case
SELECT setval('road_nodes_node_id_seq', (SELECT MAX(node_id) FROM road_nodes));

-- Arêtes (Routes connectant les nœuds)
-- Distances approximatives et vitesses réalistes (ex: N3 lourde = 60km/h, Autoroute = 90km/h)

INSERT INTO road_edges (source_node_id, target_node_id, road_name, distance_km, max_speed_kmh, travel_time_seconds, road_type) VALUES
-- Douala -> Edea (N3)
(2, 3, 'N3', 60.0, 70, 3085, 'NATIONAL'), -- ~50 min
(3, 2, 'N3', 60.0, 70, 3085, 'NATIONAL'),

-- Edea -> Pouma (N3)
(3, 4, 'N3', 45.0, 80, 2025, 'NATIONAL'), -- ~35 min
(4, 3, 'N3', 45.0, 80, 2025, 'NATIONAL'),

-- Pouma -> Boumnyebel (N3)
(4, 5, 'N3', 30.0, 80, 1350, 'NATIONAL'), -- ~22 min
(5, 4, 'N3', 30.0, 80, 1350, 'NATIONAL'),

-- Boumnyebel -> Yaoundé (N3)
(5, 6, 'N3', 90.0, 70, 4628, 'NATIONAL'), -- ~1h15
(6, 5, 'N3', 90.0, 70, 4628, 'NATIONAL'),

-- Douala Centre -> Sortie Est
(1, 2, 'Sortie Est', 10.0, 40, 900, 'URBAN'),
(2, 1, 'Entrée Douala', 10.0, 40, 900, 'URBAN'),

-- Edea -> Kribi (N7)
(3, 16, 'N7', 100.0, 80, 4500, 'NATIONAL'), -- 1h15
(16, 3, 'N7', 100.0, 80, 4500, 'NATIONAL'),

-- Douala -> Nkongsamba (N5)
(1, 15, 'N5', 70.0, 60, 4200, 'NATIONAL'), -- Loum
(15, 1, 'N5', 70.0, 60, 4200, 'NATIONAL'),
(15, 14, 'N5', 30.0, 60, 1800, 'NATIONAL'), -- Nkongsamba
(14, 15, 'N5', 30.0, 60, 1800, 'NATIONAL'),

-- Nkongsamba -> Bafang -> Bafoussam
(14, 13, 'N5', 50.0, 50, 3600, 'NATIONAL'), -- Bafang
(13, 14, 'N5', 50.0, 50, 3600, 'NATIONAL'),
(13, 11, 'N5', 60.0, 50, 4320, 'NATIONAL'), -- Bafoussam
(11, 13, 'N5', 60.0, 50, 4320, 'NATIONAL'),

-- Yaoundé -> Bafoussam (N4)
(6, 7, 'Sortie Nord', 15.0, 40, 1350, 'URBAN'),
(7, 6, 'Entrée Yaoundé', 15.0, 40, 1350, 'URBAN'),
(7, 8, 'N4', 35.0, 70, 1800, 'NATIONAL'), -- Obala
(8, 7, 'N4', 35.0, 70, 1800, 'NATIONAL'),
(8, 9, 'N4', 80.0, 80, 3600, 'NATIONAL'), -- Bafia
(9, 8, 'N4', 80.0, 80, 3600, 'NATIONAL'),
(9, 10, 'N4', 40.0, 70, 2057, 'NATIONAL'), -- Makenene
(10, 9, 'N4', 40.0, 70, 2057, 'NATIONAL'),
(10, 11, 'N4', 90.0, 60, 5400, 'NATIONAL'), -- Bafoussam
(11, 10, 'N4', 90.0, 60, 5400, 'NATIONAL'),

-- Bafoussam -> Dschang
(11, 12, 'N6', 50.0, 50, 3600, 'NATIONAL'),
(12, 11, 'N6', 50.0, 50, 3600, 'NATIONAL'),

-- Douala -> Tiko -> Mutengene
(1, 19, 'N3', 25.0, 60, 1500, 'NATIONAL'),
(19, 1, 'N3', 25.0, 60, 1500, 'NATIONAL'),
(19, 20, 'N3', 10.0, 60, 600, 'NATIONAL'),
(20, 19, 'N3', 10.0, 60, 600, 'NATIONAL'),

-- Mutengene -> Buea
(20, 18, 'Buea Road', 15.0, 50, 1080, 'NATIONAL'),
(18, 20, 'Buea Road', 15.0, 50, 1080, 'NATIONAL'),

-- Mutengene -> Limbé
(20, 17, 'N3', 15.0, 70, 770, 'NATIONAL'),
(17, 20, 'N3', 15.0, 70, 770, 'NATIONAL'),

-- Yaoundé -> Bertoua (N10)
(7, 24, 'N10', 330.0, 70, 17000, 'NATIONAL'),
(24, 7, 'N10', 330.0, 70, 17000, 'NATIONAL'),

-- Bertoua -> Ngaoundéré (N1)
(24, 23, 'N1', 500.0, 60, 30000, 'NATIONAL'),
(23, 24, 'N1', 500.0, 60, 30000, 'NATIONAL'),

-- Ngaoundéré -> Garoua (N1)
(23, 21, 'N1', 280.0, 70, 14400, 'NATIONAL'),
(21, 23, 'N1', 280.0, 70, 14400, 'NATIONAL'),

-- Garoua -> Maroua (N1)
(21, 22, 'N1', 210.0, 70, 10800, 'NATIONAL'),
(22, 21, 'N1', 210.0, 70, 10800, 'NATIONAL');

-- Update geometry automatically via trigger, but ensuring time_cost is correct
UPDATE road_edges SET travel_time_seconds = (distance_km / max_speed_kmh * 3600)::INT;

-- 5. METTRE À JOUR GÉOMÉTRIE POSTGIS
UPDATE pois SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) WHERE geom IS NULL;

-- 6. NŒUDS ROUTIERS (Graphe simplifié Yaoundé-Douala)
INSERT INTO road_nodes (node_id, latitude, longitude, node_type) VALUES
(1, 3.8667, 11.5167, 'intersection'), -- Yaoundé Centre
(2, 3.8500, 11.4900, 'intersection'), -- Melen
(3, 3.8000, 10.1300, 'intersection'), -- Edéa
(4, 4.0511, 9.7679, 'intersection')   -- Douala Centre
ON CONFLICT (node_id) DO NOTHING;

-- Mettre à jour les géométries
UPDATE road_nodes SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) WHERE geom IS NULL;

-- 7. ARÊTES ROUTIÈRES
INSERT INTO road_edges (source_node_id, target_node_id, distance_km, max_speed_kmh, road_type, road_name, travel_time_seconds, one_way) VALUES
(1, 2, 3.0, 50, 'primary', 'Boulevard du 20 Mai', 216, false),
(2, 1, 3.0, 50, 'primary', 'Boulevard du 20 Mai', 216, false),
(2, 3, 130.0, 90, 'motorway', 'Nationale N3', 5200, false),
(3, 2, 130.0, 90, 'motorway', 'Nationale N3', 5200, false),
(3, 4, 60.0, 90, 'motorway', 'Nationale N3', 2400, false),
(4, 3, 60.0, 90, 'motorway', 'Nationale N3', 2400, false)
ON CONFLICT DO NOTHING;

-- Mettre à jour les géométries des arêtes
UPDATE road_edges e
SET geom = ST_MakeLine(s.geom, t.geom)
FROM road_nodes s, road_nodes t
WHERE e.source_node_id = s.node_id 
AND e.target_node_id = t.node_id 
AND e.geom IS NULL;

-- ================================================================
-- RÉSUMÉ DES DONNÉES
-- ================================================================

DO $$
DECLARE
    v_users INT;
    v_categories INT;
    v_pois INT;
    v_nodes INT;
    v_edges INT;
    v_plannings INT;
    v_itineraries INT;
BEGIN
    SELECT COUNT(*) INTO v_users FROM users;
    SELECT COUNT(*) INTO v_categories FROM poi_categories;
    SELECT COUNT(*) INTO v_pois FROM pois;
    SELECT COUNT(*) INTO v_nodes FROM road_nodes;
    SELECT COUNT(*) INTO v_edges FROM road_edges;
    SELECT COUNT(*) INTO v_plannings FROM plannings;
    SELECT COUNT(*) INTO v_itineraries FROM itineraries;
    
    RAISE NOTICE '========================================';
    RAISE NOTICE 'BASE DE DONNÉES INITIALISÉE AVEC SUCCÈS';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Utilisateurs: %', v_users;
    RAISE NOTICE 'Catégories POI: %', v_categories;
    RAISE NOTICE 'Points d''Intérêt: %', v_pois;
    RAISE NOTICE 'Nœuds routiers: %', v_nodes;
    RAISE NOTICE 'Arêtes routières: %', v_edges;
    RAISE NOTICE 'Planifications: %', v_plannings;
    RAISE NOTICE 'Itinéraires: %', v_itineraries;
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Connexions:';
    RAISE NOTICE 'Admin: admin@planningmap.cm / password123';
    RAISE NOTICE 'User:  user@planningmap.cm / password123';
    RAISE NOTICE '========================================';
END $$;

-- ================================================================
-- INSERTION DE DONNÉES POUR TESTER LE CALCUL D'ITINÉRAIRE
-- ================================================================

-- 1. CRÉER DES POINTS SUPPLÉMENTAIRE POUR LES TESTS
INSERT INTO pois (poi_id, name, description, latitude, longitude, category_id, address_city, rating) VALUES
-- Yaoundé
(101, 'Messa Carrefour', 'Carrefour principal de Messa', 3.8680, 11.5180, (SELECT category_id FROM poi_categories WHERE name = 'Station-service'), 'Yaoundé', 3.8),
(102, 'Stade Ahmadou Ahidjo', 'Stade principal de Yaoundé', 3.8700, 11.5220, (SELECT category_id FROM poi_categories WHERE name = 'Attraction touristique'), 'Yaoundé', 4.2),
(103, 'Sortie Sud Yaoundé', 'Point de sortie vers route Douala', 3.8500, 11.4500, (SELECT category_id FROM poi_categories WHERE name = 'Station-service'), 'Yaoundé', 3.5),
(104, 'Mbankomo', 'Arrêt Mbankomo', 3.7800, 11.2000, (SELECT category_id FROM poi_categories WHERE name = 'Station-service'), 'Mbankomo', 3.0),
(105, 'Pont sur la Sanaga', 'Pont traversant la rivière Sanaga', 3.8050, 10.1200, (SELECT category_id FROM poi_categories WHERE name = 'Attraction touristique'), 'Edéa', 4.0),
(106, 'Aéroport de Douala', 'Aéroport international', 4.0080, 9.7200, (SELECT category_id FROM poi_categories WHERE name = 'Gare'), 'Douala', 4.1),
(107, 'Carrefour Deïdo', 'Carrefour commercial important', 4.0530, 9.7620, (SELECT category_id FROM poi_categories WHERE name = 'Attraction touristique'), 'Douala', 3.7),

-- Kribi
(41, 'Hôtel Ilomba', 'Resort calme en bord de mer', 2.9350, 9.9100, (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Kribi', 4.5),
(42, 'Restaurant Les Chutes', 'Restaurant avec vue sur les chutes', 2.9300, 9.9050, (SELECT category_id FROM poi_categories WHERE name = 'Restaurant'), 'Kribi', 4.2),
(43, 'Chutes de la Lobé', 'Chutes se jetant dans la mer', 2.9200, 9.9000, (SELECT category_id FROM poi_categories WHERE name = 'Attraction touristique'), 'Kribi', 4.8),

-- Limbé
(44, 'Seme Beach Hotel', 'Hôtel avec plage volcanique', 4.0150, 9.1650, (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Limbé', 4.3),
(45, 'Jardin Botanique de Limbé', 'Grand jardin botanique historique', 4.0100, 9.2000, (SELECT category_id FROM poi_categories WHERE name = 'Attraction touristique'), 'Limbé', 4.6),
(11, 'Pullman Douala Rabingha', 'Hôtel de luxe au coeur de Bonanjo', 4.0450, 9.6950, (SELECT category_id FROM poi_categories WHERE name = 'Hébergement'), 'Douala', 4.4)
ON CONFLICT (poi_id) DO NOTHING;

-- Mettre à jour la géométrie
UPDATE pois SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) WHERE geom IS NULL;

-- 2. CRÉER DES NŒUDS ROUTIERS SUPPLÉMENTAIRES POUR LE CALCUL D'ITINÉRAIRE
INSERT INTO road_nodes (node_id, latitude, longitude, node_type) VALUES
-- Points intermédiaires pour meilleure précision
(101, 3.8680, 11.5180, 'junction'), -- Messa
(102, 3.8750, 11.5250, 'junction'), -- Vers stade
(103, 3.8500, 11.4500, 'junction'), -- Sortie Sud Yaoundé
(104, 3.7800, 11.2000, 'junction'), -- Mbankomo
(105, 3.8050, 10.1200, 'junction'), -- Pont Sanaga
(106, 4.0080, 9.7200, 'junction'),  -- Aéroport Douala
(107, 4.0400, 9.7500, 'junction')   -- Entrée Douala
ON CONFLICT (node_id) DO NOTHING;

-- Mettre à jour les géométries des nœuds
UPDATE road_nodes SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) WHERE geom IS NULL;

-- 3. CRÉER UN GRAPHE ROUTIER PLUS DÉTAILLÉ POUR LE CALCUL D'ITINÉRAIRE
INSERT INTO road_edges (source_node_id, target_node_id, distance_km, max_speed_kmh, road_type, road_name, travel_time_seconds, one_way) VALUES
-- Yaoundé interne
(1, 101, 0.5, 40, 'secondary', 'Rue Messa', 45, false),
(101, 1, 0.5, 40, 'secondary', 'Rue Messa', 45, false),
(101, 102, 0.8, 40, 'secondary', 'Avenue du Stade', 72, false),
(102, 101, 0.8, 40, 'secondary', 'Avenue du Stade', 72, false),

-- Route Yaoundé - Edéa (détaillée)
(2, 103, 5.0, 70, 'primary', 'Sortie Sud', 257, false),
(103, 2, 5.0, 70, 'primary', 'Sortie Sud', 257, false),
(103, 104, 15.0, 90, 'motorway', 'RN3', 600, false),
(104, 103, 15.0, 90, 'motorway', 'RN3', 600, false),
(104, 3, 115.0, 90, 'motorway', 'RN3', 4600, false),
(3, 104, 115.0, 90, 'motorway', 'RN3', 4600, false),

-- Route Edéa - Douala (détaillée)
(3, 105, 2.5, 50, 'primary', 'Pont Sanaga', 180, false),
(105, 3, 2.5, 50, 'primary', 'Pont Sanaga', 180, false),
(105, 106, 55.0, 90, 'motorway', 'RN3', 2200, false),
(106, 105, 55.0, 90, 'motorway', 'RN3', 2200, false),
(106, 107, 5.0, 60, 'primary', 'Route Aéroport', 300, false),
(107, 106, 5.0, 60, 'primary', 'Route Aéroport', 300, false),
(107, 4, 4.0, 50, 'primary', 'Boulevard Liberté', 288, false),
(4, 107, 4.0, 50, 'primary', 'Boulevard Liberté', 288, false)
ON CONFLICT DO NOTHING;

-- Mettre à jour les géométries des arêtes
UPDATE road_edges e
SET geom = ST_MakeLine(s.geom, t.geom)
FROM road_nodes s, road_nodes t
WHERE e.source_node_id = s.node_id 
AND e.target_node_id = t.node_id 
AND e.geom IS NULL;

-- 4. CRÉER DES ITINÉRAIRES DE TEST
INSERT INTO itineraries (id, name, user_id, origin_location, destination_location, waypoints_json, distance_meters, duration_seconds) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Yaoundé - Douala Express', 
 '11111111-1111-1111-1111-111111111111', 'Yaoundé Centre', 'Douala Centre',
 '[{"name": "Edéa", "lat": 3.8000, "lng": 10.1300}]', 245000, 10800),
 
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Tour de Yaoundé', 
 '22222222-2222-2222-2222-222222222222', 'Messa', 'Stade Ahidjo',
 '[{"name": "Marché Central", "lat": 3.8640, "lng": 11.5140}]', 3500, 1200),
 
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Circuit Touristique', 
 '11111111-1111-1111-1111-111111111111', 'Hilton Yaoundé', 'Port de Douala',
 '[{"name": "Edéa Pont", "lat": 3.8050, "lng": 10.1200}, {"name": "Aéroport Douala", "lat": 4.0080, "lng": 9.7200}]', 250000, 11000)
ON CONFLICT (id) DO NOTHING;

-- 5. CRÉER DES PLANIFICATIONS COMPLÈTES
INSERT INTO plannings (id, name, user_id, status) VALUES
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Voyage d''affaires Yaoundé-Douala', 
 '11111111-1111-1111-1111-111111111111', 'PLANNED'),
 
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Week-end touristique', 
 '22222222-2222-2222-2222-222222222222', 'DRAFT'),
 
('ffffffff-ffff-ffff-ffff-ffffffffffff', 'Mission professionnelle complète', 
 '11111111-1111-1111-1111-111111111111', 'IN_PROGRESS')
ON CONFLICT (id) DO NOTHING;

-- 6. CRÉER DES ÉLÉMENTS DE PLANIFICATION LIÉS AUX ITINÉRAIRES
INSERT INTO planning_items (id, planning_id, origin_city, destination_city, planned_date, departure_time, 
                          distance_meters, travel_time_seconds, status, itinerary_id) VALUES
-- Voyage d'affaires Yaoundé-Douala
('11111111-0000-0000-0000-000000000001', 'dddddddd-dddd-dddd-dddd-dddddddddddd',
 'Yaoundé', 'Douala', '2024-12-20', '08:00:00', 245000, 10800, 'CONFIRMED',
 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
 
('11111111-0000-0000-0000-000000000002', 'dddddddd-dddd-dddd-dddd-dddddddddddd',
 'Douala', 'Yaoundé', '2024-12-22', '16:00:00', 245000, 10800, 'PENDING',
 NULL),
 
-- Week-end touristique
('22222222-0000-0000-0000-000000000001', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
 'Yaoundé', 'Yaoundé', '2024-12-25', '10:00:00', 3500, 1200, 'PLANNED',
 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
 
-- Mission professionnelle
('33333333-0000-0000-0000-000000000001', 'ffffffff-ffff-ffff-ffff-ffffffffffff',
 'Yaoundé', 'Douala', '2024-12-28', '07:30:00', 250000, 11000, 'IN_PROGRESS',
 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
 
('33333333-0000-0000-0000-000000000002', 'ffffffff-ffff-ffff-ffff-ffffffffffff',
 'Douala', 'Edéa', '2024-12-29', '09:00:00', 60000, 2400, 'PENDING',
 NULL)
ON CONFLICT (id) DO NOTHING;

-- 7. CRÉER DES VOYAGES COMPLETS (TRIPS) POUR TESTER LES FONCTIONNALITÉS AVANCÉES
INSERT INTO trips (trip_id, title, description, start_date, end_date, status, 
                  is_public, owner_id, total_distance_km, total_duration_minutes) VALUES
('aaaaaaaa-0000-0000-0000-000000000001', 'Road Trip Cameroun', 
 'Voyage touristique à travers le Cameroun', '2024-12-15', '2024-12-20', 'PLANNED',
 TRUE, '11111111-1111-1111-1111-111111111111', 450.5, 480),
 
('bbbbbbbb-0000-0000-0000-000000000001', 'Mission d''affaires Yaoundé', 
 'Rencontres professionnelles à Yaoundé', '2024-12-18', '2024-12-19', 'IN_PROGRESS',
 FALSE, '22222222-2222-2222-2222-222222222222', 50.2, 120),
 
('cccccccc-0000-0000-0000-000000000001', 'Circuit Douala Littoral', 
 'Visite des sites touristiques de Douala', '2024-12-22', '2024-12-23', 'DRAFT',
 TRUE, '11111111-1111-1111-1111-111111111111', 35.7, 180)
ON CONFLICT (trip_id) DO NOTHING;

-- 8. CRÉER DES ÉTAPES DE VOYAGE (TRIP_WAYPOINTS) AVEC POIS
INSERT INTO trip_waypoints (trip_id, order_index, waypoint_type, poi_id, 
                           planned_arrival_time, planned_duration_minutes) VALUES
-- Road Trip Cameroun
('aaaaaaaa-0000-0000-0000-000000000001', 1, 'START', 1, '2024-12-15 08:00:00', 60),
('aaaaaaaa-0000-0000-0000-000000000001', 2, 'WAYPOINT', 3, '2024-12-15 09:30:00', 30),
('aaaaaaaa-0000-0000-0000-000000000001', 3, 'WAYPOINT', 104, '2024-12-15 12:00:00', 90),
('aaaaaaaa-0000-0000-0000-000000000001', 4, 'END', 12, '2024-12-15 16:00:00', 120),

-- Mission d'affaires Yaoundé
('bbbbbbbb-0000-0000-0000-000000000001', 1, 'START', 1, '2024-12-18 09:00:00', 90),
('bbbbbbbb-0000-0000-0000-000000000001', 2, 'WAYPOINT', 103, '2024-12-18 11:00:00', 120),
('bbbbbbbb-0000-0000-0000-000000000001', 3, 'WAYPOINT', 102, '2024-12-18 14:00:00', 60),
('bbbbbbbb-0000-0000-0000-000000000001', 4, 'END', 2, '2024-12-18 16:00:00', 90),

-- Circuit Douala
('cccccccc-0000-0000-0000-000000000001', 1, 'START', 12, '2024-12-22 10:00:00', 60),
('cccccccc-0000-0000-0000-000000000001', 2, 'WAYPOINT', 11, '2024-12-22 12:00:00', 120),
('cccccccc-0000-0000-0000-000000000001', 3, 'WAYPOINT', 105, '2024-12-22 15:00:00', 90),
('cccccccc-0000-0000-0000-000000000001', 4, 'END', 106, '2024-12-22 17:00:00', 60)
ON CONFLICT DO NOTHING;

-- 9. AJOUTER DES MEMBRES AUX VOYAGES COLLABORATIFS
INSERT INTO trip_members (trip_id, user_id, role) VALUES
-- Admin est owner de tous les voyages qu'il a créés
('aaaaaaaa-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'OWNER'),
('aaaaaaaa-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'EDITOR'),

-- User est owner de son voyage
('bbbbbbbb-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'OWNER'),
('bbbbbbbb-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'VIEWER'),

-- Admin invite user à son voyage
('cccccccc-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'OWNER'),
('cccccccc-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'EDITOR')
ON CONFLICT DO NOTHING;

-- ================================================================
-- FONCTION POUR TESTER LE CALCUL D'ITINÉRAIRE
-- ================================================================

-- Fonction pour trouver l'itinéraire le plus court entre deux POIs
CREATE OR REPLACE FUNCTION find_shortest_path_between_pois(
    start_poi_id BIGINT,
    end_poi_id BIGINT
) RETURNS TABLE (
    edge_id BIGINT,
    source_lat DECIMAL,
    source_lng DECIMAL,
    target_lat DECIMAL,
    target_lng DECIMAL,
    distance_km DECIMAL,
    travel_time_seconds INT,
    road_name VARCHAR
) AS $$
DECLARE
    start_node_id BIGINT;
    end_node_id BIGINT;
BEGIN
    -- Trouver les nœuds routiers les plus proches des POIs
    SELECT node_id INTO start_node_id
    FROM road_nodes
    ORDER BY ST_Distance(
        geom,
        (SELECT geom FROM pois WHERE poi_id = start_poi_id)
    ) ASC
    LIMIT 1;
    
    SELECT node_id INTO end_node_id
    FROM road_nodes
    ORDER BY ST_Distance(
        geom,
        (SELECT geom FROM pois WHERE poi_id = end_poi_id)
    ) ASC
    LIMIT 1;
    
    -- Retourner le chemin (simplifié pour l'exemple)
    RETURN QUERY
    WITH RECURSIVE path AS (
        SELECT 
            e.edge_id,
            s.latitude as source_lat,
            s.longitude as source_lng,
            t.latitude as target_lat,
            t.longitude as target_lng,
            e.distance_km,
            e.travel_time_seconds,
            e.road_name,
            e.target_node_id,
            1 as depth
        FROM road_edges e
        JOIN road_nodes s ON e.source_node_id = s.node_id
        JOIN road_nodes t ON e.target_node_id = t.node_id
        WHERE e.source_node_id = start_node_id
        
        UNION ALL
        
        SELECT 
            e.edge_id,
            s.latitude as source_lat,
            s.longitude as source_lng,
            t.latitude as target_lat,
            t.longitude as target_lng,
            e.distance_km,
            e.travel_time_seconds,
            e.road_name,
            e.target_node_id,
            p.depth + 1
        FROM road_edges e
        JOIN road_nodes s ON e.source_node_id = s.node_id
        JOIN road_nodes t ON e.target_node_id = t.node_id
        JOIN path p ON e.source_node_id = p.target_node_id
        WHERE p.depth < 10  -- Limite de profondeur pour éviter les boucles infinies
    )
    SELECT 
        p.edge_id,
        p.source_lat,
        p.source_lng,
        p.target_lat,
        p.target_lng,
        p.distance_km,
        p.travel_time_seconds,
        p.road_name
    FROM path p
    WHERE p.target_node_id = end_node_id
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- Script de migration pour corriger la table "pois"
-- À exécuter sur la base de données si vous ne souhaitez pas la réinitialiser complètement.

-- 1. Ajouter les colonnes manquantes
ALTER TABLE pois ADD COLUMN IF NOT EXISTS type VARCHAR(50);
ALTER TABLE pois ADD COLUMN IF NOT EXISTS address_country VARCHAR(100) DEFAULT 'Cameroun';
ALTER TABLE pois ADD COLUMN IF NOT EXISTS price_range VARCHAR(50);
ALTER TABLE pois ADD COLUMN IF NOT EXISTS images JSONB;

-- 2. Convertir les colonnes TEXT[] en JSONB
-- Note: Cette conversion suppose que les colonnes sont vides ou contiennent des données compatibles via CAST.
-- Si vous avez des données, le CAST direct TEXT[] -> JSONB n'est pas trivial en une commande simple sans fonction custom.
-- Pour une base de développement, on drop et recreate les colonnes si nécessaire, ou on utilise une astuce avec to_jsonb.

-- Services
ALTER TABLE pois ALTER COLUMN services TYPE JSONB USING to_jsonb(services);
-- Amenities
ALTER TABLE pois ALTER COLUMN amenities TYPE JSONB USING to_jsonb(amenities);
-- Tags
ALTER TABLE pois ALTER COLUMN tags TYPE JSONB USING to_jsonb(tags);

-- Vérifiez d'abord la structure actuelle de votre table
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'pois';

-- Créez la table avec toutes les colonnes si elle n'existe pas, ou ajoutez les colonnes manquantes
ALTER TABLE pois 
ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS view_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS favorite_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS services JSONB,
ADD COLUMN IF NOT EXISTS amenities JSONB,
ADD COLUMN IF NOT EXISTS tags JSONB,
ADD COLUMN IF NOT EXISTS images JSONB,
ADD COLUMN IF NOT EXISTS metadata JSONB,
ADD COLUMN IF NOT EXISTS type VARCHAR(50),
ADD COLUMN IF NOT EXISTS created_by UUID,
ADD COLUMN IF NOT EXISTS email VARCHAR(255),
ADD COLUMN IF NOT EXISTS website VARCHAR(255),
ADD COLUMN IF NOT EXISTS price_range VARCHAR(10),
ADD COLUMN IF NOT EXISTS address_postal_code VARCHAR(20),
ADD COLUMN IF NOT EXISTS address_region VARCHAR(100),
ADD COLUMN IF NOT EXISTS address_neighborhood VARCHAR(100),
ADD COLUMN IF NOT EXISTS address_country VARCHAR(100) DEFAULT 'Cameroun';


-- ================================================================
-- 1. CRÉER DES PLANNINGS
-- ================================================================

-- Planning 1: Yaoundé → Douala (Route principale)
INSERT INTO plannings (
    id, name, user_id, status, created_at, updated_at
) VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    'Yaoundé - Douala Express',
    '22222222-2222-2222-2222-222222222222'::uuid,
    'FINALIZED',
    NOW(),
    NOW()
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status;

-- Planning 2: Circuit Touristique Sud
INSERT INTO plannings (
    id, name, user_id, status, created_at, updated_at
) VALUES (
    '22222222-2222-2222-2222-222222222222'::uuid,
    'Circuit Balnéaire Sud',
    '22222222-2222-2222-2222-222222222222'::uuid,
    'DRAFT',
    NOW(),
    NOW()
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status;

-- Planning 3: Tour Gastronomique Yaoundé
INSERT INTO plannings (
    id, name, user_id, status, created_at, updated_at
) VALUES (
    '33333333-3333-3333-3333-333333333333'::uuid,
    'Tour Gastronomique Yaoundé',
    '22222222-2222-2222-2222-222222222222'::uuid,
    'FINALIZED',
    NOW(),
    NOW()
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status;

-- Planning 4: Road Trip Grand Nord
INSERT INTO plannings (
    id, name, user_id, status, created_at, updated_at
) VALUES (
    '44444444-4444-4444-4444-444444444444'::uuid,
    'Aventure Grand Nord',
    '22222222-2222-2222-2222-222222222222'::uuid,
    'DRAFT',
    NOW(),
    NOW()
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status;

-- Planning 5: Week-end Détente Douala
INSERT INTO plannings (
    id, name, user_id, status, created_at, updated_at
) VALUES (
    '55555555-5555-5555-5555-555555555555'::uuid,
    'Week-end Détente Douala',
    '22222222-2222-2222-2222-222222222222'::uuid,
    'FINALIZED',
    NOW(),
    NOW()
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status;

    -- 3. TRIGGERS

CREATE OR REPLACE FUNCTION trigger_update_geom() RETURNS TRIGGER AS $$
BEGIN
    NEW.geom = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pois_geom BEFORE INSERT OR UPDATE OF latitude, longitude ON pois FOR EACH ROW EXECUTE FUNCTION trigger_update_geom();
CREATE TRIGGER trg_road_nodes_geom BEFORE INSERT OR UPDATE OF latitude, longitude ON road_nodes FOR EACH ROW EXECUTE FUNCTION trigger_update_geom();

CREATE OR REPLACE FUNCTION trigger_update_timestamp() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_u_ts BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION trigger_update_timestamp();
CREATE TRIGGER trg_p_ts BEFORE UPDATE ON pois FOR EACH ROW EXECUTE FUNCTION trigger_update_timestamp();
CREATE TRIGGER trg_rn_ts BEFORE UPDATE ON road_nodes FOR EACH ROW EXECUTE FUNCTION trigger_update_timestamp();
CREATE TRIGGER trg_re_ts BEFORE UPDATE ON road_edges FOR EACH ROW EXECUTE FUNCTION trigger_update_timestamp();
CREATE TRIGGER trg_reviews_ts BEFORE UPDATE ON poi_reviews FOR EACH ROW EXECUTE FUNCTION trigger_update_timestamp();
CREATE TRIGGER trg_trips_ts BEFORE UPDATE ON trips FOR EACH ROW EXECUTE FUNCTION trigger_update_timestamp();
CREATE TRIGGER trg_waypoints_ts BEFORE UPDATE ON trip_waypoints FOR EACH ROW EXECUTE FUNCTION trigger_update_timestamp();

-- ================================================================
-- 2. CRÉER DES PLANNING ITEMS (TRAJETS)
-- ================================================================

-- Planning 1: Yaoundé → Douala
INSERT INTO planning_items (
    id, planning_id,
    origin_city, destination_city,
    planned_date, departure_time,
    origin_latitude, origin_longitude,
    destination_latitude, destination_longitude,
    distance_meters, travel_time_seconds,
    status, created_at, updated_at
) VALUES (
    'a1111111-1111-1111-1111-111111111111'::uuid,
    '11111111-1111-1111-1111-111111111111'::uuid,
    'Yaoundé', 'Douala',
    CURRENT_DATE, '08:00:00',
    3.8667, 11.5167,  -- Yaoundé
    4.0511, 9.7679,   -- Douala
    250500, 14400,    -- 250.5 km, 4h
    'CALCULATED',
    NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

-- Planning 2: Circuit Balnéaire (3 trajets)
INSERT INTO planning_items (
    id, planning_id,
    origin_city, destination_city,
    planned_date, departure_time,
    origin_latitude, origin_longitude,
    destination_latitude, destination_longitude,
    distance_meters, travel_time_seconds,
    status, created_at, updated_at
) VALUES
    -- Douala → Kribi
    (
        'b1111111-1111-1111-1111-111111111111'::uuid,
        '22222222-2222-2222-2222-222222222222'::uuid,
        'Douala', 'Kribi',
        CURRENT_DATE + INTERVAL '7 days', '09:00:00',
        4.0511, 9.7679,   -- Douala
        2.9378, 9.9078,   -- Kribi
        150000, 10800,    -- 150 km, 3h
        'CALCULATED',
        NOW(), NOW()
    ),
    -- Kribi → Limbe
    (
        'b2222222-2222-2222-2222-222222222222'::uuid,
        '22222222-2222-2222-2222-222222222222'::uuid,
        'Kribi', 'Limbe',
        CURRENT_DATE + INTERVAL '9 days', '10:00:00',
        2.9378, 9.9078,   -- Kribi
        4.0167, 9.2167,   -- Limbe
        200000, 14400,    -- 200 km, 4h
        'CALCULATED',
        NOW(), NOW()
    ),
    -- Limbe → Douala
    (
        'b3333333-3333-3333-3333-333333333333'::uuid,
        '22222222-2222-2222-2222-222222222222'::uuid,
        'Limbe', 'Douala',
        CURRENT_DATE + INTERVAL '10 days', '16:00:00',
        4.0167, 9.2167,   -- Limbe
        4.0511, 9.7679,   -- Douala
        75000, 5400,      -- 75 km, 1h30
        'CALCULATED',
        NOW(), NOW()
    )
ON CONFLICT (id) DO NOTHING;

-- Planning 3: Tour Gastronomique (trajets courts dans Yaoundé)
INSERT INTO planning_items (
    id, planning_id,
    origin_city, destination_city,
    planned_date, departure_time,
    origin_latitude, origin_longitude,
    destination_latitude, destination_longitude,
    distance_meters, travel_time_seconds,
    status, created_at, updated_at
) VALUES
    (
        'c1111111-1111-1111-1111-111111111111'::uuid,
        '33333333-3333-3333-3333-333333333333'::uuid,
        'Yaoundé Centre', 'Bastos',
        CURRENT_DATE + INTERVAL '3 days', '12:00:00',
        3.8667, 11.5167,  -- Centre
        3.8700, 11.5180,  -- Bastos
        5000, 900,        -- 5 km, 15min
        'CALCULATED',
        NOW(), NOW()
    ),
    (
        'c2222222-2222-2222-2222-222222222222'::uuid,
        '33333333-3333-3333-3333-333333333333'::uuid,
        'Bastos', 'Messa',
        CURRENT_DATE + INTERVAL '3 days', '14:00:00',
        3.8700, 11.5180,  -- Bastos
        3.8650, 11.5150,  -- Messa
        3000, 600,        -- 3 km, 10min
        'CALCULATED',
        NOW(), NOW()
    )
ON CONFLICT (id) DO NOTHING;

-- Planning 4: Grand Nord (4 trajets)
INSERT INTO planning_items (
    id, planning_id,
    origin_city, destination_city,
    planned_date, departure_time,
    origin_latitude, origin_longitude,
    destination_latitude, destination_longitude,
    distance_meters, travel_time_seconds,
    status, created_at, updated_at
) VALUES
    -- Yaoundé → Bafoussam
    (
        'd1111111-1111-1111-1111-111111111111'::uuid,
        '44444444-4444-4444-4444-444444444444'::uuid,
        'Yaoundé', 'Bafoussam',
        CURRENT_DATE + INTERVAL '14 days', '07:00:00',
        3.8667, 11.5167,  -- Yaoundé
        5.4781, 10.4178,  -- Bafoussam
        280000, 18000,    -- 280 km, 5h
        'CALCULATED',
        NOW(), NOW()
    ),
    -- Bafoussam → Garoua
    (
        'd2222222-2222-2222-2222-222222222222'::uuid,
        '44444444-4444-4444-4444-444444444444'::uuid,
        'Bafoussam', 'Garoua',
        CURRENT_DATE + INTERVAL '16 days', '08:00:00',
        5.4781, 10.4178,  -- Bafoussam
        9.3012, 13.3964,  -- Garoua
        450000, 28800,    -- 450 km, 8h
        'CALCULATED',
        NOW(), NOW()
    ),
    -- Garoua → Bamenda
    (
        'd3333333-3333-3333-3333-333333333333'::uuid,
        '44444444-4444-4444-4444-444444444444'::uuid,
        'Garoua', 'Bamenda',
        CURRENT_DATE + INTERVAL '19 days', '09:00:00',
        9.3012, 13.3964,  -- Garoua
        5.9597, 10.1453,  -- Bamenda
        520000, 32400,    -- 520 km, 9h
        'PENDING',
        NOW(), NOW()
    )
ON CONFLICT (id) DO NOTHING;

-- Planning 5: Week-end Douala (trajets courts)
INSERT INTO planning_items (
    id, planning_id,
    origin_city, destination_city,
    planned_date, departure_time,
    origin_latitude, origin_longitude,
    destination_latitude, destination_longitude,
    distance_meters, travel_time_seconds,
    status, created_at, updated_at
) VALUES
    (
        'e1111111-1111-1111-1111-111111111111'::uuid,
        '55555555-5555-5555-5555-555555555555'::uuid,
        'Douala Centre', 'Akwa',
        CURRENT_DATE + INTERVAL '5 days', '10:00:00',
        4.0511, 9.7679,   -- Centre
        4.0520, 9.7690,   -- Akwa
        2000, 600,        -- 2 km, 10min
        'CALCULATED',
        NOW(), NOW()
    )
ON CONFLICT (id) DO NOTHING;

-- ================================================================
-- 3. CRÉER TABLE D'ASSOCIATION PLANNING-POI (si elle n'existe pas)
-- ================================================================

CREATE TABLE IF NOT EXISTS planning_pois (
    planning_id UUID NOT NULL REFERENCES plannings(id) ON DELETE CASCADE,
    poi_id BIGINT NOT NULL REFERENCES pois(poi_id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT NOW(),
    notes TEXT,
    PRIMARY KEY (planning_id, poi_id)
);

-- ================================================================
-- 4. ASSOCIER DES POI AUX PLANNINGS
-- ================================================================

-- Planning 1: Yaoundé → Douala (POI le long de la route)
INSERT INTO planning_pois (planning_id, poi_id, notes) VALUES
    -- Yaoundé
    ('11111111-1111-1111-1111-111111111111'::uuid, 1, 'Petit-déjeuner avant départ'),  -- Hôtel Hilton
    ('11111111-1111-1111-1111-111111111111'::uuid, 7, 'Plein d''essence'),  -- Total Bastos
    -- Douala
    ('11111111-1111-1111-1111-111111111111'::uuid, 11, 'Déjeuner arrivée'),  -- Pullman Douala
    ('11111111-1111-1111-1111-111111111111'::uuid, 15, 'Station Douala')  -- Total Akwa
ON CONFLICT (planning_id, poi_id) DO NOTHING;

-- Planning 2: Circuit Balnéaire (POI touristiques)
INSERT INTO planning_pois (planning_id, poi_id, notes) VALUES
    ('22222222-2222-2222-2222-222222222222'::uuid, 11, 'Hôtel départ Douala'),  -- Pullman
    ('22222222-2222-2222-2222-222222222222'::uuid, 41, 'Resort Kribi'),  -- Hôtel Ilomba
    ('22222222-2222-2222-2222-222222222222'::uuid, 42, 'Restaurant vue chutes'),  -- Les Chutes
    ('22222222-2222-2222-2222-222222222222'::uuid, 43, 'Attraction principale'),  -- Chutes Lobé
    ('22222222-2222-2222-2222-222222222222'::uuid, 44, 'Hôtel Limbe'),  -- Seme Beach
    ('22222222-2222-2222-2222-222222222222'::uuid, 45, 'Visite jardin')  -- Jardin Botanique
ON CONFLICT (planning_id, poi_id) DO NOTHING;

-- Planning 3: Tour Gastronomique (Restaurants uniquement)
INSERT INTO planning_pois (planning_id, poi_id, notes) VALUES
    ('33333333-3333-3333-3333-333333333333'::uuid, 4, 'Déjeuner gastronomique'),  -- La Terrasse
    ('33333333-3333-3333-3333-333333333333'::uuid, 5, 'Cuisine locale'),  -- Chez Wou
    ('33333333-3333-3333-3333-333333333333'::uuid, 6, 'Dîner italien')  -- Pizza Napoli
ON CONFLICT (planning_id, poi_id) DO NOTHING;

-- Planning 4: Grand Nord (Hébergements et restaurants)
INSERT INTO planning_pois (planning_id, poi_id, notes) VALUES
    ('44444444-4444-4444-4444-444444444444'::uuid, 1, 'Départ Yaoundé'),
    ('44444444-4444-4444-4444-444444444444'::uuid, 26, 'Hôtel Bafoussam'),  -- Talotel
    ('44444444-4444-4444-4444-444444444444'::uuid, 27, 'Restaurant Bafoussam'),  -- Le Palanka
    ('44444444-4444-4444-4444-444444444444'::uuid, 29, 'Hôtel Garoua'),  -- Ribadou
    ('44444444-4444-4444-4444-444444444444'::uuid, 30, 'Restaurant Garoua'),  -- Le Sahel
    ('44444444-4444-4444-4444-444444444444'::uuid, 31, 'Hôtel Bamenda'),  -- Ayaba
    ('44444444-4444-4444-4444-444444444444'::uuid, 32, 'Restaurant Bamenda')  -- Dreamland
ON CONFLICT (planning_id, poi_id) DO NOTHING;

-- Planning 5: Week-end Douala (Détente et culture)
INSERT INTO planning_pois (planning_id, poi_id, notes) VALUES
    ('55555555-5555-5555-5555-555555555555'::uuid, 11, 'Hébergement'),  -- Pullman
    ('55555555-5555-5555-5555-555555555555'::uuid, 13, 'Restaurant gastronomique'),  -- Le Biniou
    ('55555555-5555-5555-5555-555555555555'::uuid, 14, 'Restaurant fruits de mer'),  -- Poissonnerie
    ('55555555-5555-5555-5555-555555555555'::uuid, 18, 'Visite culturelle'),  -- Cathédrale
    ('55555555-5555-5555-5555-555555555555'::uuid, 19, 'Shopping artisanal')  -- Marché des Fleurs
ON CONFLICT (planning_id, poi_id) DO NOTHING;

-- ================================================================
-- VÉRIFICATION
-- ================================================================

-- Afficher les plannings créés
SELECT
    p.name AS planning,
    p.status,
    COUNT(DISTINCT pi.id) AS nb_trajets,
    COUNT(DISTINCT pp.poi_id) AS nb_pois,
    SUM(pi.distance_meters)/1000 AS distance_totale_km,
    SUM(pi.travel_time_seconds)/3600 AS duree_totale_h
FROM plannings p
LEFT JOIN planning_items pi ON p.id = pi.planning_id
LEFT JOIN planning_pois pp ON p.id = pp.planning_id
GROUP BY p.id, p.name, p.status, p.created_at
ORDER BY p.created_at;

-- Afficher les POI par planning
SELECT
    p.name AS planning,
    poi.name AS poi,
    c.name AS categorie,
    poi.address_city AS ville,
    pp.notes
FROM planning_pois pp
JOIN plannings p ON pp.planning_id = p.id
JOIN pois poi ON pp.poi_id = poi.poi_id
LEFT JOIN poi_categories c ON poi.category_id = c.category_id
ORDER BY p.name, poi.name;

COMMIT;

-- ===================================================
-- Script de mise à jour: Ajout types de notifications
-- Exécuter ce script pour mettre à jour la base existante
-- ===================================================

-- Modifier la contrainte CHECK pour ajouter les nouveaux types
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_type_check 
    CHECK (type IN (
        'TRIP_INVITE', 'TRIP_UPDATE', 'WAYPOINT_ADDED', 'MEMBER_JOINED',
        'POI_VERIFIED', 'REVIEW_REPLY', 'SYSTEM_ALERT', 'NEW_POI', 'NEW_USER'
    ));

-- Afficher confirmation
DO $$
BEGIN
    RAISE NOTICE '======================================';
    RAISE NOTICE 'Contrainte de type notification mise à jour';
    RAISE NOTICE 'Nouveaux types ajoutés: NEW_POI, NEW_USER';
    RAISE NOTICE '======================================';
END $$;


-- ================================================================
-- FIN DU SCRIPT
-- ================================================================

