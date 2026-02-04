-- ===================================================
-- Script d'Initialisation COMPLET de la Base de Données
-- Système de Planification d'Itinéraires avec POI
-- Auteur: Thomas Djotio Ndié, Prof Dr_Eng.
-- Date: 19 Décembre 2024
-- Version: 2.0 COMPLETE
-- ===================================================

-- Supprimer les tables existantes (ATTENTION: Perte de données)
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS trip_members CASCADE;
DROP TABLE IF EXISTS trip_waypoints CASCADE;
DROP TABLE IF EXISTS trips CASCADE;
DROP TABLE IF EXISTS poi_reviews CASCADE;
DROP TABLE IF EXISTS pois CASCADE;
DROP TABLE IF EXISTS poi_categories CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS road_nodes CASCADE;
DROP TABLE IF EXISTS road_edges CASCADE;

-- Activer l'extension PostGIS pour les données géospatiales
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; -- Pour générer des UUID

-- ===================================================
-- 1. TABLE: users (Utilisateurs)
-- ===================================================
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

-- ===================================================
-- 2. TABLE: poi_categories (Catégories de POI)
-- ===================================================
CREATE TABLE poi_categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(7),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================
-- 3. TABLE: pois (Points d'Intérêt)
-- ===================================================
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

    -- Contact
    phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(255),

    -- Informations complémentaires
    rating DECIMAL(3, 2) DEFAULT 0.0 CHECK (rating >= 0 AND rating <= 5),
    review_count INT DEFAULT 0,
    opening_hours JSONB, -- Format: {"monday": "09:00-18:00", ...}
    services TEXT[], -- Array PostgreSQL pour les services
    amenities TEXT[], -- Équipements (wifi, parking, etc.)
    price_level INT CHECK (price_level >= 1 AND price_level <= 4),
    tags TEXT[],

    -- Image
    image_url VARCHAR(500),

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

-- ===================================================
-- 4. TABLE: poi_reviews (Avis sur POI)
-- ===================================================
CREATE TABLE poi_reviews (
    review_id BIGSERIAL PRIMARY KEY,
    poi_id BIGINT NOT NULL REFERENCES pois(poi_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rating DECIMAL(2, 1) NOT NULL CHECK (rating >= 0 AND rating <= 5),
    comment TEXT,
    images TEXT[], -- URLs des images
    is_verified_visit BOOLEAN DEFAULT FALSE,
    helpful_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Un utilisateur ne peut noter qu'une fois un POI
    UNIQUE(poi_id, user_id)
);

CREATE INDEX idx_reviews_poi_id ON poi_reviews(poi_id);
CREATE INDEX idx_reviews_user_id ON poi_reviews(user_id);
CREATE INDEX idx_reviews_rating ON poi_reviews(rating DESC);
CREATE INDEX idx_reviews_created_at ON poi_reviews(created_at DESC);

-- ===================================================
-- 5. TABLE: trips (Voyages)
-- ===================================================
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trips_owner_id ON trips(owner_id);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_share_token ON trips(share_token) WHERE share_token IS NOT NULL;
CREATE INDEX idx_trips_start_date ON trips(start_date);
CREATE INDEX idx_trips_is_public ON trips(is_public) WHERE is_public = TRUE;

-- ===================================================
-- 6. TABLE: trip_waypoints (Étapes de voyage)
-- ===================================================
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

-- ===================================================
-- 7. TABLE: trip_members (Membres collaborateurs)
-- ===================================================
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

-- ===================================================
-- 8. TABLE: notifications (Notifications utilisateur)
-- ===================================================
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,

    -- Type de notification
    type VARCHAR(50) NOT NULL CHECK (type IN (
        'TRIP_INVITE', 'TRIP_UPDATE', 'WAYPOINT_ADDED', 'MEMBER_JOINED',
        'POI_VERIFIED', 'REVIEW_REPLY', 'SYSTEM_ALERT'
    )),

    -- Contenu
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,

    -- Entité associée
    related_entity_type VARCHAR(50), -- 'TRIP', 'POI', 'USER'
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

-- ===================================================
-- 9. TABLE: road_nodes (Nœuds du graphe routier)
-- ===================================================
CREATE TABLE road_nodes (
    node_id BIGSERIAL PRIMARY KEY,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    geom GEOMETRY(Point, 4326),

    -- Métadonnées OSM
    osm_id BIGINT,
    node_type VARCHAR(50), -- 'intersection', 'poi', 'waypoint'
    tags JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_road_nodes_geom ON road_nodes USING GIST(geom);
CREATE INDEX idx_road_nodes_location ON road_nodes(latitude, longitude);
CREATE UNIQUE INDEX idx_road_nodes_osm_id ON road_nodes(osm_id) WHERE osm_id IS NOT NULL;

-- ===================================================
-- 10. TABLE: road_edges (Arêtes du graphe routier)
-- ===================================================
CREATE TABLE road_edges (
    edge_id BIGSERIAL PRIMARY KEY,
    source_node_id BIGINT NOT NULL REFERENCES road_nodes(node_id) ON DELETE CASCADE,
    target_node_id BIGINT NOT NULL REFERENCES road_nodes(node_id) ON DELETE CASCADE,

    -- Propriétés de la route
    distance_km DECIMAL(10, 3) NOT NULL,
    max_speed_kmh INT,
    road_type VARCHAR(50), -- 'motorway', 'primary', 'secondary', 'tertiary'
    road_name VARCHAR(255),
    is_oneway BOOLEAN DEFAULT FALSE,

    -- Coûts pour A*
    time_cost_seconds INT, -- distance / vitesse

    -- Métadonnées OSM
    osm_way_id BIGINT,
    tags JSONB,

    -- Géométrie de la route
    geom GEOMETRY(LineString, 4326),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_road_edges_source ON road_edges(source_node_id);
CREATE INDEX idx_road_edges_target ON road_edges(target_node_id);
CREATE INDEX idx_road_edges_geom ON road_edges USING GIST(geom);
CREATE INDEX idx_road_edges_road_type ON road_edges(road_type);

-- ===================================================
-- TRIGGERS: Mise à jour automatique updated_at
-- ===================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

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

-- ===================================================
-- TRIGGERS: Géométrie PostGIS automatique
-- ===================================================

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

-- ===================================================
-- TRIGGERS: Statistiques automatiques
-- ===================================================

-- Recalculer rating moyen POI après ajout/suppression avis
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

-- ===================================================
-- TRIGGERS: Génération token partage automatique
-- ===================================================

CREATE OR REPLACE FUNCTION generate_share_token()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_public = TRUE AND NEW.share_token IS NULL THEN
        NEW.share_token = encode(gen_random_bytes(16), 'hex');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER generate_share_token_trigger
    BEFORE INSERT OR UPDATE OF is_public ON trips
    FOR EACH ROW EXECUTE FUNCTION generate_share_token();

-- ===================================================
-- FONCTIONS UTILITAIRES
-- ===================================================

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

-- ===================================================
-- VUES MATÉRIALISÉES (Performances)
-- ===================================================

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

-- ===================================================
-- DONNÉES INITIALES
-- ===================================================

-- Catégories POI
INSERT INTO poi_categories (name, description, icon, color) VALUES
('Hébergement', 'Hôtels, motels, auberges, campings', 'hotel', '#3498DB'),
('Restaurant', 'Restaurants, cafés, bars, fast-food', 'restaurant', '#E67E22'),
('Péage', 'Barrages de péage routiers', 'toll', '#95A5A6'),
('Station-service', 'Stations-essence et services automobiles', 'local_gas_station', '#E74C3C'),
('Attraction touristique', 'Sites touristiques, monuments, musées', 'attractions', '#9B59B6'),
('Hôpital', 'Centres médicaux, hôpitaux, cliniques', 'local_hospital', '#E67E22'),
('Parking', 'Aires de stationnement', 'local_parking', '#7F8C8D'),
('Aire de repos', 'Aires de repos, haltes routières', 'local_cafe', '#16A085'),
('Gare', 'Gares ferroviaires et routières', 'train', '#34495E'),
('Banque', 'Banques et distributeurs ATM', 'account_balance', '#27AE60')
ON CONFLICT (name) DO NOTHING;

-- Utilisateur admin par défaut (mot de passe: admin123)
INSERT INTO users (username, email, password_hash, role, company_name, phone, city, transportmode) VALUES
('admin', 'admin@planningmap.cm', '$2a$10$xPzLKxTEqxNQwCgJhZx8O.vN8c5z8jKrJ3X4HhQqX8dVqYvGxOK4m', 'ADMIN', 'Planning Map Corp', '+237600000000', 'Yaoundé', 'CAR'),
('demo_user', 'demo@planningmap.cm', '$2a$10$xPzLKxTEqxNQwCgJhZx8O.vN8c5z8jKrJ3X4HhQqX8dVqYvGxOK4m', 'USER', NULL, '+237699112233', 'Douala', 'WALK')
ON CONFLICT (email) DO NOTHING;

-- POI Yaoundé
INSERT INTO pois (name, description, latitude, longitude, category_id, address_street, address_city, phone, rating, opening_hours, services, amenities, price_level, image_url) VALUES
('Hotel Hilton Yaoundé', 'Hôtel 5 étoiles au centre-ville avec vue panoramique', 3.8667, 11.5167, 1, 'Boulevard du 20 Mai', 'Yaoundé', '+237222234656', 4.5,
 '{"monday": "00:00-23:59", "tuesday": "00:00-23:59", "wednesday": "00:00-23:59", "thursday": "00:00-23:59", "friday": "00:00-23:59", "saturday": "00:00-23:59", "sunday": "00:00-23:59"}',
 ARRAY['wifi', 'parking', 'restaurant', 'room_service'],
 ARRAY['piscine', 'gym', 'spa', 'business_center', 'conference_rooms'],
 4, 'https://images.unsplash.com/photo-1566073771259-6a8506099945'),

('Station Total Melen', 'Station-service 24/7 avec boutique', 3.8500, 11.4900, 4, 'Quartier Melen', 'Yaoundé', '+237670123456', 4.0,
 '{"monday": "00:00-23:59", "tuesday": "00:00-23:59", "wednesday": "00:00-23:59", "thursday": "00:00-23:59", "friday": "00:00-23:59", "saturday": "00:00-23:59", "sunday": "00:00-23:59"}',
 ARRAY['fuel', 'shop', 'restroom', 'atm'],
 ARRAY['car_wash', 'air_pump', 'wifi'],
 1, 'https://images.unsplash.com/photo-1545262810-77515befe149'),

('Restaurant Le Biniou', 'Restaurant français gastronomique', 3.8700, 11.5200, 2, 'Quartier Bastos', 'Yaoundé', '+237222205050', 4.7,
 '{"monday": "12:00-15:00,19:00-23:00", "tuesday": "12:00-15:00,19:00-23:00", "wednesday": "12:00-15:00,19:00-23:00", "thursday": "12:00-15:00,19:00-23:00", "friday": "12:00-15:00,19:00-23:00", "saturday": "12:00-15:00,19:00-23:00", "sunday": "Fermé"}',
 ARRAY['wifi', 'parking', 'takeout', 'delivery'],
 ARRAY['air_conditioning', 'terrasse', 'wheelchair_accessible'],
 3, 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4');

-- POI Douala
INSERT INTO pois (name, description, latitude, longitude, category_id, address_street, address_city, phone, rating, opening_hours, services, amenities, price_level) VALUES
('Pullman Douala Rabingha', 'Hôtel de luxe 5 étoiles', 4.0511, 9.7679, 1, 'Boulevard de la Liberté, Akwa', 'Douala', '+237233424649', 4.6,
 '{"monday": "00:00-23:59", "tuesday": "00:00-23:59", "wednesday": "00:00-23:59", "thursday": "00:00-23:59", "friday": "00:00-23:59", "saturday": "00:00-23:59", "sunday": "00:00-23:59"}',
 ARRAY['wifi', 'parking', 'restaurant', 'room_service', 'concierge'],
 ARRAY['piscine', 'gym', 'spa', 'business_center', 'bar'],
 4),

('Péage Edéa', 'Poste de péage principal RN3', 3.8000, 10.1300, 3, 'Route Nationale 3', 'Edéa', NULL, 3.5,
 '{"monday": "00:00-23:59", "tuesday": "00:00-23:59", "wednesday": "00:00-23:59", "thursday": "00:00-23:59", "friday": "00:00-23:59", "saturday": "00:00-23:59", "sunday": "00:00-23:59"}',
 ARRAY['restroom'],
 ARRAY[],
 NULL);

-- ===================================================
-- RÉSUMÉ DES DONNÉES
-- ===================================================

DO $$
DECLARE
    v_users INT;
    v_categories INT;
    v_pois INT;
    v_reviews INT;
    v_trips INT;
    v_nodes INT;
    v_edges INT;
BEGIN
    SELECT COUNT(*) INTO v_users FROM users;
    SELECT COUNT(*) INTO v_categories FROM poi_categories;
    SELECT COUNT(*) INTO v_pois FROM pois;
    SELECT COUNT(*) INTO v_reviews FROM poi_reviews;
    SELECT COUNT(*) INTO v_trips FROM trips;
    SELECT COUNT(*) INTO v_nodes FROM road_nodes;
    SELECT COUNT(*) INTO v_edges FROM road_edges;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'BASE DE DONNÉES INITIALISÉE AVEC SUCCÈS';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Utilisateurs: %', v_users;
    RAISE NOTICE 'Catégories POI: %', v_categories;
    RAISE NOTICE 'Points d''Intérêt: %', v_pois;
    RAISE NOTICE 'Avis POI: %', v_reviews;
    RAISE NOTICE 'Voyages: %', v_trips;
    RAISE NOTICE 'Nœuds routiers: %', v_nodes;
    RAISE NOTICE 'Arêtes routières: %', v_edges;
    RAISE NOTICE '========================================';
END $$;

-- ===================================================
-- FIN DU SCRIPT
-- ===================================================