-- ================================================================
-- Migration V5: Fix POI Reviews Schema
-- ================================================================

-- 1. Ajouter les colonnes manquantes pour la modération et les signalements
ALTER TABLE poi_reviews ADD COLUMN IF NOT EXISTS is_moderated BOOLEAN;
ALTER TABLE poi_reviews ADD COLUMN IF NOT EXISTS report_count INT DEFAULT 0;

-- 2. S'assurer que le type de images est compatible (TEXT[] est déjà correct d'après init.sql)
-- Mais nous allons nous assurer qu'on utilise bien TEXT[] pour éviter les soucis de mapping JSON vs Array
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'poi_reviews' AND column_name = 'images') THEN
        -- Déjà présent, vérification du type faite par Hibernate/R2DBC
    ELSE
        ALTER TABLE poi_reviews ADD COLUMN images TEXT[];
    END IF;
END $$;
