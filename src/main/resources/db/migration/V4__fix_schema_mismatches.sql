-- ================================================================
-- Migration V4: Fix Schema Mismatches & Ensure Consistency
-- ================================================================

-- 1. Fix PoiCategory
ALTER TABLE poi_categories ADD COLUMN IF NOT EXISTS parent_category_id BIGINT;

-- 2. Fix RoadEdge (Rename column if old name exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'road_edges' AND column_name = 'time_cost_seconds') THEN
        ALTER TABLE road_edges RENAME COLUMN time_cost_seconds TO travel_time_seconds;
    END IF;
END $$;

-- 3. Ensure Planning Tables Exist (Idempotent checks)
CREATE TABLE IF NOT EXISTS plannings (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_planning_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS planning_items (
    id UUID PRIMARY KEY,
    planning_id UUID NOT NULL,
    origin_city VARCHAR(255) NOT NULL,
    destination_city VARCHAR(255) NOT NULL,
    planned_date DATE NOT NULL,
    departure_time TIME NOT NULL,
    distance_meters DOUBLE PRECISION,
    travel_time_seconds INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    route_geom GEOMETRY(LineString, 4326),
    -- V3 columns
    optimal_route_geom TEXT,
    itinerary_id UUID,
    selected_waypoints_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_planning FOREIGN KEY (planning_id) REFERENCES plannings(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_plannings_user_id ON plannings(user_id);
CREATE INDEX IF NOT EXISTS idx_planning_items_planning_id ON planning_items(planning_id);

-- 4. Create Itineraries if not exists
CREATE TABLE IF NOT EXISTS itineraries (
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

-- FK for Itinerary in Items
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_planning_items_itinerary') THEN
        ALTER TABLE planning_items
            ADD CONSTRAINT fk_planning_items_itinerary
            FOREIGN KEY (itinerary_id)
            REFERENCES itineraries(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- 5. Triggers
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'update_updated_at_column') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_update_plannings_updated_at') THEN
            CREATE TRIGGER tr_update_plannings_updated_at BEFORE UPDATE ON plannings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_update_planning_items_updated_at') THEN
            CREATE TRIGGER tr_update_planning_items_updated_at BEFORE UPDATE ON planning_items FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_update_itineraries_updated_at') THEN
            CREATE TRIGGER tr_update_itineraries_updated_at BEFORE UPDATE ON itineraries FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
        END IF;
    END IF;
END $$;
