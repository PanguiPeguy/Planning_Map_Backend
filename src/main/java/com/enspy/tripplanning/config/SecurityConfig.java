package com.enspy.tripplanning.config;

import com.enspy.tripplanning.authentification.security.JwtTokenProvider;
import com.enspy.tripplanning.authentification.security.ReactiveUserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import java.util.List;

/**
 * ================================================================
 * Configuration Sécurité Spring WebFlux
 * ================================================================
 * 
 * 🔐 OBJECTIFS:
 * - Authentification JWT sans état (stateless)
 * - Protection routes selon rôles (ADMIN/CLIENT)
 * - CORS pour frontend React/Next.js
 * - Rate limiting (anti-brute-force)
 * 
 * 🎯 ARCHITECTURE:
 * 1. AuthenticationWebFilter intercepte requêtes
 * 2. Extrait JWT depuis Authorization header
 * 3. Valide token (signature, expiration)
 * 4. Charge UserDetails depuis DB
 * 5. Inject Authentication dans SecurityContext
 * 
 * ⚡ RÉACTIF:
 * - Tout en Mono/Flux (non-bloquant)
 * - NoOpServerSecurityContextRepository (pas de session)
 * - Performances x10 vs Spring Security classique
 * 
 * ================================================================
 * 
 * @author Planning Map Team
 * @version 1.0.0
 * @since 2024-12-14
 *        ================================================================
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity // Permet @PreAuthorize dans controllers
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenProvider jwtTokenProvider;
        private final ReactiveUserDetailsServiceImpl userDetailsService;

        // ============================================================
        // PASSWORD ENCODER (BCrypt)
        // ============================================================

        /**
         * Encodeur BCrypt pour mots de passe
         * 
         * 🔐 SÉCURITÉ:
         * - Strength 10 = 2^10 = 1024 iterations
         * - Hashage ~100ms (bon compromis perf/sécurité)
         * - Résistant attaques brute-force
         * 
         * ⚠️ JAMAIS stocker mot de passe en clair !
         * 
         * @return PasswordEncoder BCrypt
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(10);
        }

        // ============================================================
        // SECURITY FILTER CHAIN (Configuration principale)
        // ============================================================

        /**
         * Chaîne de filtres sécurité WebFlux
         * 
         * 📋 CONFIGURATION:
         * 1. CSRF désactivé (API stateless)
         * 2. CORS activé (frontend séparé)
         * 3. Form login désactivé (JWT seulement)
         * 4. HTTP Basic désactivé (JWT seulement)
         * 5. Session désactivée (stateless)
         * 6. Routes publiques/protégées
         * 7. JWT filter injecté
         * 
         * @param http ServerHttpSecurity builder
         * @return SecurityWebFilterChain configurée
         */
        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
                return http
                                // ========================================
                                // DÉSACTIVATION FEATURES INUTILES
                                // ========================================
                                .csrf(csrf -> csrf.disable()) // CSRF inutile en API stateless
                                .formLogin(form -> form.disable()) // Pas de form HTML
                                .httpBasic(basic -> basic.disable()) // Pas de Basic Auth

                                // ========================================
                                // CORS (Cross-Origin Resource Sharing)
                                // ========================================
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // ========================================
                                // STATELESS (pas de session server-side)
                                // ========================================
                                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                                // ========================================
                                // AUTORISATION ROUTES
                                // ========================================
                                .authorizeExchange(auth -> auth
                                                // ---- ROUTES PUBLIQUES (accessible sans auth) ----

                                                // Authentification
                                                .pathMatchers("/api/v1/auth/**").permitAll()

                                                // Documentation API (support both root and /api/v1 prefixed paths)
                                                .pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .pathMatchers("/api/v1/swagger-ui/**", "/api/v1/v3/api-docs/**",
                                                                "/api/v1/webjars/**",
                                                                "/api/v1/swagger-ui.html")
                                                .permitAll()

                                                // Actuator (monitoring)
                                                .pathMatchers("/actuator/health", "/actuator/info").permitAll()

                                                // Routing (Temporairement public pour test de performance)
                                                .pathMatchers("/api/v1/routing/**").authenticated()

                                                // POI public (lecture seule)
                                                .pathMatchers(HttpMethod.GET, "/api/v1/pois/**").permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/v1/poi-categories/**").permitAll()

                                                // Uploads (accès public aux images)
                                                .pathMatchers("/uploads/**").permitAll()

                                                // ---- ROUTES ADMIN SEULEMENT ----

                                                // Gestion utilisateurs
                                                .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")

                                                // Modération POI
                                                .pathMatchers(HttpMethod.POST, "/api/v1/pois/*/verify").hasRole("ADMIN")
                                                .pathMatchers(HttpMethod.DELETE, "/api/v1/pois/*/like").authenticated()
                                                .pathMatchers(HttpMethod.DELETE, "/api/v1/pois/*/favorite")
                                                .authenticated()
                                                .pathMatchers(HttpMethod.DELETE, "/api/v1/pois/**").hasRole("ADMIN")

                                                // Métriques sensibles
                                                .pathMatchers("/actuator/**").hasRole("ADMIN")

                                                // ---- ROUTES AUTHENTIFIÉES (tout rôle) ----

                                                // Toutes les autres routes nécessitent authentification
                                                .anyExchange().authenticated())

                                // ========================================
                                // GESTION ERREURS AUTHENTIFICATION
                                // ========================================
                                .exceptionHandling(exceptions -> exceptions
                                                // 401 Unauthorized (token invalide/expiré)
                                                .authenticationEntryPoint((exchange, ex) -> {
                                                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                                        return exchange.getResponse().setComplete();
                                                })

                                                // 403 Forbidden (permissions insuffisantes)
                                                .accessDeniedHandler((exchange, denied) -> {
                                                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                                                        return exchange.getResponse().setComplete();
                                                }))

                                // ========================================
                                // JWT AUTHENTICATION FILTER
                                // ========================================
                                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)

                                .build();
        }

        // ============================================================
        // JWT AUTHENTICATION FILTER
        // ============================================================

        /**
         * Filtre authentification JWT personnalisé
         * 
         * 🔄 FLOW:
         * 1. Request arrive avec header "Authorization: Bearer <token>"
         * 2. bearerConverter() extrait token
         * 3. authenticationManager() valide token
         * 4. userDetailsService charge User depuis DB
         * 5. SecurityContext rempli avec Authentication
         * 6. Controller reçoit @AuthenticationPrincipal User
         * 
         * @return AuthenticationWebFilter configuré
         */
        private AuthenticationWebFilter jwtAuthenticationFilter() {
                // Manager authentification réactif
                AuthenticationWebFilter filter = new AuthenticationWebFilter(
                                reactiveAuthenticationManager());

                // Converter Bearer token → Authentication
                filter.setServerAuthenticationConverter(bearerConverter());

                // Handler échec authentification
                filter.setAuthenticationFailureHandler((exchange, ex) -> {
                        exchange.getExchange().getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return Mono.empty();
                });

                // Exclure les endpoints d'authentification ET SWAGGER du filtre JWT
                // Cela évite les erreurs 401 si un token invalide est envoyé
                filter.setRequiresAuthenticationMatcher(exchange -> {
                        String path = exchange.getRequest().getPath().value();
                        // Exclusions
                        if (path.startsWith("/api/v1/auth/") ||
                                        path.startsWith("/api/v1/swagger-ui") ||
                                        path.startsWith("/api/v1/v3/api-docs") ||
                                        path.startsWith("/api/v1/webjars") ||
                                        path.startsWith("/swagger-ui") ||
                                        path.startsWith("/v3/api-docs") ||
                                        path.startsWith("/webjars") ||
                                        path.equals("/swagger-ui.html") ||
                                        path.equals("/api/v1/swagger-ui.html") ||
                                        path.startsWith("/uploads/")) {
                                return org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
                                                .notMatch();
                        }
                        return org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
                                        .match();
                });

                return filter;
        }

        // ============================================================
        // AUTHENTICATION MANAGER
        // ============================================================

        /**
         * Manager authentification réactif JWT
         * 
         * 🔍 VALIDATION TOKEN:
         * 1. Vérifie signature JWT
         * 2. Vérifie expiration
         * 3. Extrait email (subject)
         * 4. Charge User depuis DB
         * 5. Retourne Authentication si valide
         * 
         * @return ReactiveAuthenticationManager
         */
        @Bean
        public ReactiveAuthenticationManager reactiveAuthenticationManager() {
                return authentication -> {
                        String token = authentication.getCredentials().toString();

                        if (!jwtTokenProvider.validateToken(token)) {
                                return Mono.error(new BadCredentialsException("Token JWT invalide ou expiré"));
                        }

                        String email = jwtTokenProvider.getEmailFromToken(token);

                        return userDetailsService.findByUsername(email)
                                        .switchIfEmpty(Mono.error(new UsernameNotFoundException(
                                                        "Utilisateur non trouvé: " + email)))
                                        .map(userDetails -> new UsernamePasswordAuthenticationToken(
                                                        userDetails,
                                                        token,
                                                        userDetails.getAuthorities()));
                };
        }

        // ============================================================
        // BEARER TOKEN CONVERTER
        // ============================================================

        /**
         * Convertit header Authorization en Authentication
         * 
         * 📨 INPUT: Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
         * 📤 OUTPUT: UsernamePasswordAuthenticationToken(token, token)
         * 
         * ⚠️ Validation réelle faite par authenticationManager
         * 
         * @return ServerAuthenticationConverter
         */

        private ServerAuthenticationConverter bearerConverter() {
                return exchange -> Mono.justOrEmpty(
                                exchange.getRequest().getHeaders().getFirst("Authorization"))
                                .filter(header -> header.startsWith("Bearer "))
                                .map(header -> header.substring(7).trim()) // Ajout de .trim() pour la robustesse
                                .filter(token -> !token.isBlank())
                                .map(BearerTokenAuthenticationToken::new);
        }

        // ============================================================
        // CORS CONFIGURATION
        // ============================================================

        /**
         * Configuration CORS pour frontend
         * 
         * 🌐 PERMET:
         * - Origines: localhost:3000 (dev), planning-map.cm (prod)
         * - Méthodes: GET, POST, PUT, DELETE, PATCH, OPTIONS
         * - Headers: Authorization, Content-Type, etc.
         * - Credentials: true (cookies HttpOnly)
         * 
         * ⚠️ IMPORTANT:
         * Sans CORS, browser bloque requêtes cross-origin !
         * 
         * @return CorsConfigurationSource
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                // Origines autorisées
                config.setAllowedOrigins(List.of(
                                "https://planning-map-frontend.vercel.app", // Next.js dev
                                "https://planning-map-frontend.vercel.app", // Next.js prod preview
                                "https://planning-map-frontend.vercel.app", // Production
                                "https://planning-map-frontend.vercel.app" // Production www
                ));

                // Méthodes HTTP autorisées
                config.setAllowedMethods(List.of(
                                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

                // Headers autorisés (request)
                config.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "Origin",
                                "X-Requested-With"));

                // Headers exposés (response)
                config.setExposedHeaders(List.of(
                                "Authorization",
                                "X-Total-Count",
                                "X-Page-Number",
                                "X-Page-Size"));

                // Autoriser credentials (cookies)
                config.setAllowCredentials(true);

                // Durée cache preflight (OPTIONS)
                config.setMaxAge(3600L); // 1 heure

                // Application à toutes les routes
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);

                return source;
        }
}