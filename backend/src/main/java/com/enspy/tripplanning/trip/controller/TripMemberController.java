package com.enspy.tripplanning.trip.controller;

import com.enspy.tripplanning.trip.dto.*;
import com.enspy.tripplanning.trip.service.TripMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/trips/{tripId}/members")
@RequiredArgsConstructor
@Tag(name = "Trip Members", description = "Gestion collaboration voyages")
public class TripMemberController {

    private final TripMemberService memberService;

    @Operation(summary = "Inviter un membre", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TripMemberDTO> inviteMember(
        @PathVariable UUID tripId,
        @Valid @RequestBody InviteMemberRequest request,
        Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        return memberService.inviteMember(tripId, request, userId);
    }

    @Operation(summary = "Lister les membres", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public Flux<TripMemberDTO> getMembers(@PathVariable UUID tripId) {
        return memberService.getTripMembers(tripId);
    }

    @Operation(summary = "Modifier rôle membre", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{memberId}/role")
    public Mono<TripMemberDTO> updateMemberRole(
        @PathVariable UUID tripId,
        @PathVariable UUID memberId,
        @Valid @RequestBody UpdateMemberRoleRequest request,
        Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        return memberService.updateMemberRole(tripId, memberId, request, userId);
    }

    @Operation(summary = "Retirer un membre", security = @SecurityRequirement(name = "bearer-jwt"))
    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeMember(
        @PathVariable UUID tripId,
        @PathVariable UUID memberId,
        Authentication auth
    ) {
        UUID userId = extractUserId(auth);
        return memberService.removeMember(tripId, memberId, userId);
    }

    private UUID extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Non authentifié");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof com.enspy.tripplanning.authentification.entity.User) {
            return ((com.enspy.tripplanning.authentification.entity.User) principal).getUserId();
        }
        
        throw new RuntimeException("Type principal invalide");
    }
}