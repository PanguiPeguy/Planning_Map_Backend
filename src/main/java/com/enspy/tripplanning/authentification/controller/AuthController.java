package com.enspy.tripplanning.authentification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.enspy.tripplanning.authentification.dto.AuthRequest;
import com.enspy.tripplanning.authentification.dto.AuthResponse;
import com.enspy.tripplanning.authentification.dto.RegisterRequest;
import com.enspy.tripplanning.authentification.security.JwtTokenProvider;
import com.enspy.tripplanning.authentification.service.UserService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Endpoints d'authentification et d'inscription")
@RequiredArgsConstructor
@Validated
public class AuthController {

        private final UserService userService;
        private final JwtTokenProvider jwtTokenProvider;
        private final PasswordEncoder passwordEncoder;

        @PostMapping("/register")
        @Operation(summary = "Inscrire un nouvel utilisateur", description = "Crée un compte et retourne un token JWT valide 24h", responses = {
                        @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès"),
                        @ApiResponse(responseCode = "400", description = "Données invalides ou email déjà utilisé")
        })
        public Mono<ResponseEntity<AuthResponse>> register(
                        @Valid @RequestBody RegisterRequest req) {
                return userService.register(req)
                                .map(user -> {
                                        String token = jwtTokenProvider.createToken(user.getUserId(),
                                                        user.getUsername(),
                                                        user.getEmail());
                                        return ResponseEntity.status(HttpStatus.CREATED)
                                                        .body(new AuthResponse(user.getUserId(), user.getUsername(),
                                                                        user.getEmail(), user.getRole().name(), token));
                                });
        }

        @PostMapping("/login")
        @Operation(summary = "Connexion utilisateur", description = "Retourne un token JWT si les identifiants sont corrects", responses = {
                        @ApiResponse(responseCode = "200", description = "Connexion réussie"),
                        @ApiResponse(responseCode = "401", description = "Identifiants invalides")
        })
        public Mono<ResponseEntity<AuthResponse>> login(
                        @Valid @RequestBody AuthRequest req) {
                return userService.findByEmail(req.getEmail())
                                .filter(user -> passwordEncoder.matches(req.getPassword(), user.getPassword()))
                                .map(user -> {
                                        String token = jwtTokenProvider.createToken(user.getUserId(),
                                                        user.getUsername(),
                                                        user.getEmail());
                                        return ResponseEntity.ok(new AuthResponse(user.getUserId(), user.getUsername(),
                                                        user.getEmail(), user.getRole().name(), token));
                                })
                                .switchIfEmpty(Mono
                                                .error(new BadCredentialsException("Email ou mot de passe incorrect")));
        }
}