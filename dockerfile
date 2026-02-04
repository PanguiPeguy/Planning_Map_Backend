# Dockerfile
FROM eclipse-temurin:17-jre-alpine as base

# Installation de PostgreSQL et PostGIS
RUN apk add --no-cache \
    postgresql15 \
    postgresql15-contrib \
    postgis \
    postgresql15-postgis

# Créer répertoire pour données PostgreSQL
RUN mkdir -p /var/lib/postgresql/data && \
    chown -R postgres:postgres /var/lib/postgresql

# Copier les scripts d'initialisation
COPY database/init-scripts/ /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/*.sh

# Copier l'application Spring Boot
COPY --from=builder /app/target/*.jar /app/app.jar

# Script de démarrage qui lance PostgreSQL puis Spring Boot
COPY start-all.sh /start-all.sh
RUN chmod +x /start-all.sh

EXPOSE 5432 8080

CMD ["/start-all.sh"]