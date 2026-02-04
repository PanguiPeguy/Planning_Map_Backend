-- ================================================================
-- Migration V2: Planning Tables
-- ================================================================

-- 1. Create plannings table
CREATE TABLE IF NOT EXISTS plannings (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, FINALIZED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_planning_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 2. Create planning_items table
CREATE TABLE IF NOT EXISTS planning_items (
    id UUID PRIMARY KEY,
    planning_id UUID NOT NULL,
    origin_city VARCHAR(255) NOT NULL,
    destination_city VARCHAR(255) NOT NULL,
    planned_date DATE NOT NULL,
    departure_time TIME NOT NULL,
    distance_meters DOUBLE PRECISION,
    travel_time_seconds INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, CALCULATED
    route_geom GEOMETRY(LineString, 4326),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_planning FOREIGN KEY (planning_id) REFERENCES plannings(id) ON DELETE CASCADE
);

-- 3. Add indexes
CREATE INDEX idx_plannings_user_id ON plannings(user_id);
CREATE INDEX idx_planning_items_planning_id ON planning_items(planning_id);

-- 4. Enable updated_at trigger (assuming the function exists from V1)
-- Check if update_timestamp function exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'update_updated_at_column') THEN
        CREATE TRIGGER tr_update_plannings_updated_at
            BEFORE UPDATE ON plannings
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
            
        CREATE TRIGGER tr_update_planning_items_updated_at
            BEFORE UPDATE ON planning_items
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;
