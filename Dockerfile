# Dockerfile
# Étape 1: Build avec Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copier les fichiers de configuration Maven
COPY pom.xml .
COPY src ./src

# Build l'application
RUN mvn clean package -DskipTests

# Étape 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=builder /app/target/*.jar app.jar

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Port d'exposition
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]