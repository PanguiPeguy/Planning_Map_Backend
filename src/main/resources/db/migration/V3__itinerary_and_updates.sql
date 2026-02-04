-- Create Itineraries Table
CREATE TABLE IF NOT EXISTS itineraries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    origin_location VARCHAR(255),
    destination_location VARCHAR(255),
    waypoints_json TEXT, -- JSON Array
    geometry_encoded TEXT,
    distance_meters DOUBLE PRECISION,
    duration_seconds INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Update Planning Items Table
ALTER TABLE planning_items ADD COLUMN IF NOT EXISTS optimal_route_geom TEXT;
ALTER TABLE planning_items ADD COLUMN IF NOT EXISTS itinerary_id UUID;
ALTER TABLE planning_items ADD COLUMN IF NOT EXISTS selected_waypoints_json TEXT;

-- Add Foreign Key constraint for itinerary_id (optional, based on your strictness)
ALTER TABLE planning_items
    ADD CONSTRAINT fk_planning_items_itinerary
    FOREIGN KEY (itinerary_id)
    REFERENCES itineraries(id)
    ON DELETE SET NULL;
