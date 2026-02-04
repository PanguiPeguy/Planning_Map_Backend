-- ================================================================
-- Migration V6: Add Missing Columns to POIS Table
-- ================================================================

-- Ajout des compteurs
ALTER TABLE pois ADD COLUMN IF NOT EXISTS like_count INT DEFAULT 0;
ALTER TABLE pois ADD COLUMN IF NOT EXISTS favorite_count INT DEFAULT 0;
ALTER TABLE pois ADD COLUMN IF NOT EXISTS view_count INT DEFAULT 0;

-- Ajout des champs de statut et modération
ALTER TABLE pois ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE pois ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE pois ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;

-- Ajout des champs d'adresse et images
ALTER TABLE pois ADD COLUMN IF NOT EXISTS address_country VARCHAR(100) DEFAULT 'Cameroun';
ALTER TABLE pois ADD COLUMN IF NOT EXISTS images JSONB;

-- Conversion des colonnes TEXT[] en JSONB pour correspondre à l'entité Poi.java (R2DBC Json type)
-- On vérifie le type actuel pour éviter de re-convertir si déjà fait
DO $$
BEGIN
    -- Conversion services
    IF (SELECT data_type FROM information_schema.columns WHERE table_name = 'pois' AND column_name = 'services') != 'jsonb' THEN
        ALTER TABLE pois ALTER COLUMN services TYPE JSONB USING to_jsonb(services);
    END IF;
    
    -- Conversion amenities
    IF (SELECT data_type FROM information_schema.columns WHERE table_name = 'pois' AND column_name = 'amenities') != 'jsonb' THEN
        ALTER TABLE pois ALTER COLUMN amenities TYPE JSONB USING to_jsonb(amenities);
    END IF;
    
    -- Conversion tags
    IF (SELECT data_type FROM information_schema.columns WHERE table_name = 'pois' AND column_name = 'tags') != 'jsonb' THEN
        ALTER TABLE pois ALTER COLUMN tags TYPE JSONB USING to_jsonb(tags);
    END IF;
END $$;

