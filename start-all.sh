#!/bin/bash
set -e

echo "🚀 Démarrage des services..."

# Démarrer PostgreSQL
echo "🐘 Démarrage de PostgreSQL..."
su postgres -c "pg_ctl initdb -D /var/lib/postgresql/data"
su postgres -c "pg_ctl start -D /var/lib/postgresql/data -l /var/lib/postgresql/logfile"

# Attendre que PostgreSQL soit prêt
until pg_isready -h localhost -p 5432; do
  echo "⏳ En attente de PostgreSQL..."
  sleep 2
done

# Créer la base de données si elle n'existe pas
echo "📦 Initialisation de la base de données..."
su postgres -c "psql -c \"CREATE DATABASE itineraire_db;\" || true"
su postgres -c "psql -c \"CREATE EXTENSION IF NOT EXISTS postgis;\" -d itineraire_db"

# Exécuter les scripts d'initialisation
if [ -d "/docker-entrypoint-initdb.d" ]; then
  for script in /docker-entrypoint-initdb.d/*.sql; do
    echo "📄 Exécution de $script..."
    su postgres -c "psql -d itineraire_db -f $script"
  done
fi

# Démarrer Spring Boot
echo "🌱 Démarrage de Spring Boot..."
exec java -jar /app/app.jar --server.address=0.0.0.0