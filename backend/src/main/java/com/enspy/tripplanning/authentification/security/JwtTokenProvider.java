package com.enspy.tripplanning.authentification.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * JWT Token Provider - Générateur et Validateur JWT
 * ================================================================
 * 
 * 🎯 RESPONSABILITÉS:
 * - Génération tokens JWT signés (HMAC-SHA256)
 * - Validation tokens (signature, expiration)
 * - Extraction claims (email, userId, username)
 * - Refresh token management
 * 
 * 🔐 SÉCURITÉ:
 * - Secret key 512 bits minimum (HMAC-SHA256)
 * - Signature vérifiée à chaque validation
 * - Expiration obligatoire (24h access, 7j refresh)
 * - Issuer claim pour prévenir réutilisation
 * 
 * 📦 STRUCTURE TOKEN:
 * Header: {"alg": "HS256", "typ": "JWT"}
 * Payload: {"sub": "email", "userId": "uuid", ...}
 * Signature: HMACSHA256(base64(header)+"."+base64(payload), secret)
 * 
 * ================================================================
 * @author Planning Map Team
 * @version 1.0.0
 * @since 2024-12-14
 * ================================================================
 */
@Slf4j
@Component
public class JwtTokenProvider {

    // ============================================================
    // CONFIGURATION (depuis application.yaml)
    // ============================================================
    
    /**
     * Clé secrète HMAC (512 bits minimum)
     * ⚠️ CRITIQUE: Changer en production via variable environnement !
     * 
     * Génération clé sécurisée:
     * openssl rand -base64 64
     */
    @Value("${jwt.secret}")
    private String secret;
    
    /**
     * Durée validité Access Token (millisecondes)
     * Défaut: 86400000 ms = 24 heures
     */
    @Value("${jwt.expiration-ms}")
    private long expirationMs;
    
    /**
     * Durée validité Refresh Token (millisecondes)
     * Défaut: 604800000 ms = 7 jours
     */
    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;
    
    /**
     * Issuer du token (identifiant application)
     * Utilisé pour vérifier origine token
     */
    @Value("${jwt.issuer:planning-map}")
    private String issuer;

    // ============================================================
    // GÉNÉRATION CLÉ SIGNATURE
    // ============================================================
    
    /**
     * Génère la clé SecretKey depuis le secret string
     * 
     * 🔐 SÉCURITÉ:
     * - HMAC-SHA256 nécessite clé >= 256 bits
     * - Notre clé: 512 bits (recommandé)
     * - Encodage UTF-8 standard
     * 
     * @return SecretKey pour signature JWT
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ============================================================
    // GÉNÉRATION TOKENS
    // ============================================================
    
    /**
     * Crée un Access Token JWT standard
     * 
     * 📋 CLAIMS INCLUS:
     * - sub (subject): email utilisateur
     * - userId: UUID utilisateur
     * - username: pseudo utilisateur
     * - role: ADMIN ou CLIENT
     * - iss (issuer): planning-map
     * - iat (issued at): timestamp création
     * - exp (expiration): timestamp expiration
     * 
     * 🔄 UTILISÉ APRÈS:
     * - Login réussi
     * - Refresh token
     * - Vérification email
     * 
     * @param userId UUID utilisateur
     * @param username Pseudo utilisateur
     * @param email Email utilisateur (subject)
     * @param role Rôle utilisateur (ADMIN/CLIENT)
     * @return Token JWT signé
     */
    public String createToken(UUID userId, String username, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("username", username);
        claims.put("role", role);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)  // Email = identifiant principal
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    /**
     * Crée un Access Token avec rôle par défaut CLIENT
     * 
     * @param userId UUID utilisateur
     * @param username Pseudo utilisateur
     * @param email Email utilisateur
     * @return Token JWT signé
     */
    public String createToken(UUID userId, String username, String email) {
        return createToken(userId, username, email, "CLIENT");
    }
    
    /**
     * Crée un Refresh Token (longue durée)
     * 
     * 🔄 DIFFÉRENCES vs Access Token:
     * - Durée: 7 jours vs 24h
     * - Claims minimaux (juste userId + email)
     * - Type: "refresh" pour distinguer
     * 
     * 💾 STOCKAGE:
     * - Cookie HttpOnly (pas accessible JS)
     * - Table refresh_tokens DB (révocation possible)
     * 
     * @param userId UUID utilisateur
     * @param email Email utilisateur
     * @return Refresh Token JWT signé
     */
    public String createRefreshToken(UUID userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("type", "refresh");  // Identifier comme refresh
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    // ============================================================
    // VALIDATION TOKENS
    // ============================================================
    
    /**
     * Valide un token JWT complet
     * 
     * ✅ VÉRIFIE:
     * 1. Signature correcte (clé secrète)
     * 2. Pas expiré (exp claim)
     * 3. Issuer correct (planning-map)
     * 4. Format valide (header.payload.signature)
     * 
     * ❌ INVALIDE SI:
     * - Signature incorrecte (token modifié)
     * - Expiré (exp < now)
     * - Malformed (pas 3 parties)
     * - Claims manquants
     * 
     * @param token Token JWT à valider
     * @return true si valide, false sinon
     */
    public boolean validateToken(String token) {
        try {
            // Parse et vérifie signature + expiration automatiquement
            Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)  // Vérifie issuer
                .build()
                .parseClaimsJws(token);
            
            return true;
            
        } catch (SecurityException ex) {
            log.error("❌ Token JWT signature invalide: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("❌ Token JWT malformé: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("❌ Token JWT expiré: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("❌ Token JWT non supporté: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("❌ Token JWT claims vide: {}", ex.getMessage());
        }
        
        return false;
    }
    
    /**
     * Valide un Refresh Token spécifiquement
     * 
     * ✅ VÉRIFIE EN PLUS:
     * - Type claim = "refresh"
     * - Présent dans DB (pas révoqué)
     * 
     * @param token Refresh Token à valider
     * @return true si valide, false sinon
     */
    public boolean validateRefreshToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        
        try {
            Claims claims = getClaims(token);
            String type = claims.get("type", String.class);
            
            // Doit avoir type="refresh"
            return "refresh".equals(type);
            
        } catch (Exception ex) {
            log.error("❌ Erreur validation refresh token: {}", ex.getMessage());
            return false;
        }
    }

    // ============================================================
    // EXTRACTION DONNÉES (Claims)
    // ============================================================
    
    /**
     * Extrait tous les claims du token
     * 
     * ⚠️ ATTENTION: Assume token déjà validé !
     * Appeler validateToken() avant cette méthode
     * 
     * @param token Token JWT valide
     * @return Claims parsés
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    /**
     * Extrait l'email (subject) depuis token
     * 
     * 📧 EMAIL = identifiant connexion principal
     * Pas le username ! (username = pseudo affichage)
     * 
     * @param token Token JWT valide
     * @return Email utilisateur
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }
    
    /**
     * Extrait l'UUID utilisateur depuis token
     * 
     * @param token Token JWT valide
     * @return UUID utilisateur
     */
    public UUID getUserIdFromToken(String token) {
        String userIdString = getClaims(token).get("userId", String.class);
        return UUID.fromString(userIdString);
    }
    
    /**
     * Extrait le username (pseudo) depuis token
     * 
     * @param token Token JWT valide
     * @return Username utilisateur
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).get("username", String.class);
    }
    
    /**
     * Extrait le rôle depuis token
     * 
     * @param token Token JWT valide
     * @return Rôle utilisateur (ADMIN/CLIENT)
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }
    
    /**
     * Extrait la date expiration depuis token
     * 
     * @param token Token JWT valide
     * @return Date expiration
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaims(token).getExpiration();
    }
    
    /**
     * Calcule le temps restant avant expiration (secondes)
     * 
     * @param token Token JWT valide
     * @return Secondes restantes
     */
    public long getTimeUntilExpirationSeconds(String token) {
        Date expiration = getExpirationDateFromToken(token);
        Date now = new Date();
        
        long diffMs = expiration.getTime() - now.getTime();
        return diffMs / 1000;  // Convertir ms → secondes
    }
    
    /**
     * Vérifie si le token expire bientôt (< 1 heure)
     * 
     * 🔄 UTILISÉ POUR:
     * - Décider si refresh nécessaire
     * - Warning frontend "session expire soon"
     * 
     * @param token Token JWT valide
     * @return true si expire dans < 1h
     */
    public boolean isTokenExpiringSoon(String token) {
        return getTimeUntilExpirationSeconds(token) < 3600;  // < 1 heure
    }

    // ============================================================
    // UTILITAIRES
    // ============================================================
    
    /**
     * Extrait token depuis header Authorization
     * 
     * 📨 FORMAT ATTENDU: "Bearer eyJhbGciOiJIUzI1NiIs..."
     * 
     * @param authorizationHeader Header complet
     * @return Token extrait (sans "Bearer "), ou null si invalide
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
    
    /**
     * Génère un token de vérification email (simple UUID)
     * 
     * ⚠️ PAS un JWT ! Juste UUID random
     * Stocké en DB dans user.verification_token
     * 
     * @return Token UUID
     */
    public String generateEmailVerificationToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Génère un token reset password (simple UUID)
     * 
     * ⚠️ PAS un JWT ! Juste UUID random
     * Stocké en DB dans user.reset_password_token
     * Expiration: 15 minutes
     * 
     * @return Token UUID
     */
    public String generatePasswordResetToken() {
        return UUID.randomUUID().toString();
    }
}