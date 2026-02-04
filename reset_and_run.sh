#!/bin/bash

# Arrêter les conteneurs existants
echo "🛑 Arrêt des conteneurs..."
docker compose down -v

# Nettoyer les orphelins si nécessaire
docker compose down --remove-orphans

# Démarrer la base de données
echo "🚀 Démarrage de la base de données..."
docker compose up -d postgres

# Attendre que la base de données soit prête sur le port 5433
echo "⏳ Attente de la disponibilité de PostgreSQL (port 5433)..."
until nc -z localhost 5433; do
  sleep 1
  echo -n "."
done
echo ""
echo "✅ Base de données accessible !"

# Attendre un peu plus pour l'initialisation complète (init.sql)
echo "⏳ Attente de l'initialisation des données (10s)..."
sleep 10

# Compiler le backend
echo "🔨 Compilation du backend..."
./mvnw clean install -DskipTests

# Lancer l'application
echo "🏃 Lancement de l'application..."
./mvnw spring-boot:run
