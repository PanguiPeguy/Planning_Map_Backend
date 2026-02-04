package com.enspy.tripplanning.authentification.controller;

import com.enspy.tripplanning.authentification.dto.UserProfileResponse;
import com.enspy.tripplanning.authentification.entity.User;
import com.enspy.tripplanning.authentification.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Gestion des utilisateurs")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Mon profil", description = "Récupère le profil de l'utilisateur connecté", security = @SecurityRequirement(name = "bearerAuth"))
    public Mono<ResponseEntity<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal User user) {
        return userService.getUserProfile(user.getUserId())
                .map(ResponseEntity::ok);
    }

    @GetMapping
    @Operation(summary = "Liste des utilisateurs", description = "Récupère la liste de tous les utilisateurs (Admin uniquement)", security = @SecurityRequirement(name = "bearerAuth"))
    public Flux<UserProfileResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Profil utilisateur", description = "Récupère le profil d'un utilisateur par son ID")
    public Mono<ResponseEntity<UserProfileResponse>> getUserById(@PathVariable UUID id) {
        return userService.getUserProfile(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @org.springframework.web.bind.annotation.PutMapping("/me")
    @Operation(summary = "Mettre à jour mon profil", description = "Met à jour les informations du profil connecté", security = @SecurityRequirement(name = "bearerAuth"))
    public Mono<ResponseEntity<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @org.springframework.web.bind.annotation.RequestBody com.enspy.tripplanning.authentification.dto.UpdateProfileRequest request) {
        return userService.updateProfile(user.getUserId(), request)
                .map(ResponseEntity::ok);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/me")
    @Operation(summary = "Supprimer mon compte", description = "Supprime le compte de l'utilisateur connecté", security = @SecurityRequirement(name = "bearerAuth"))
    public Mono<ResponseEntity<Void>> deleteMyAccount(@AuthenticationPrincipal User user) {
        return userService.deleteUser(user.getUserId())
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un utilisateur", description = "Supprime un utilisateur par son ID (Admin uniquement)", security = @SecurityRequirement(name = "bearerAuth"))
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable UUID id) {
        return userService.deleteUser(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

}
