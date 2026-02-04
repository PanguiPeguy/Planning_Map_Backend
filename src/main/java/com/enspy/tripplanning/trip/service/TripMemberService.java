package com.enspy.tripplanning.trip.service;

import com.enspy.tripplanning.authentification.repository.UserRepository;
import com.enspy.tripplanning.trip.dto.InviteMemberRequest;
import com.enspy.tripplanning.trip.dto.TripMemberDTO;
import com.enspy.tripplanning.trip.dto.UpdateMemberRoleRequest;
import com.enspy.tripplanning.trip.entity.TripMember;
import com.enspy.tripplanning.trip.repository.TripMemberRepository;
import com.enspy.tripplanning.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripMemberService {

    private final TripMemberRepository memberRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Transactional
    public Mono<TripMemberDTO> inviteMember(UUID tripId, InviteMemberRequest request, UUID inviterId) {
        log.info("✉️ Invitation membre {} au trip {} par {}", request.getEmail(), tripId, inviterId);

        return tripRepository.findById(tripId)
            .switchIfEmpty(Mono.error(new RuntimeException("Voyage non trouvé")))
            .flatMap(trip -> {
                // Vérifier que inviter est owner ou editor
                if (!trip.isOwner(inviterId)) {
                    return memberRepository.findByTripIdAndUserId(tripId, inviterId)
                        .flatMap(member -> {
                            if (!member.canEdit()) {
                                return Mono.error(new RuntimeException("Permission refusée"));
                            }
                            return Mono.just(trip);
                        });
                }
                return Mono.just(trip);
            })
            .flatMap(trip -> userRepository.findByEmail(request.getEmail()))
            .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé: " + request.getEmail())))
            .flatMap(user -> {
                // Vérifier si déjà membre
                return memberRepository.isMember(tripId, user.getUserId())
                    .flatMap(isMember -> {
                        if (isMember) {
                            return Mono.error(new RuntimeException("Utilisateur déjà membre"));
                        }

                        TripMember member = TripMember.builder()
                            .tripId(tripId)
                            .userId(user.getUserId())
                            .role(TripMember.MemberRole.valueOf(request.getRole()))
                            .notificationsEnabled(true)
                            .build();

                        return memberRepository.save(member);
                    })
                    .map(member -> TripMemberDTO.builder()
                        .userId(member.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(member.getRole().name())
                        .joinedAt(member.getJoinedAt())
                        .notificationsEnabled(member.getNotificationsEnabled())
                        .build());
            })
            .doOnSuccess(dto -> log.info("✅ Membre {} ajouté au trip {}", dto.getEmail(), tripId));
    }

    public Flux<TripMemberDTO> getTripMembers(UUID tripId) {
        return memberRepository.findByTripId(tripId)
            .flatMap(member -> userRepository.findById(member.getUserId())
                .map(user -> TripMemberDTO.builder()
                    .userId(member.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(member.getRole().name())
                    .joinedAt(member.getJoinedAt())
                    .lastActivityAt(member.getLastActivityAt())
                    .notificationsEnabled(member.getNotificationsEnabled())
                    .build()));
    }

    @Transactional
    public Mono<TripMemberDTO> updateMemberRole(
        UUID tripId, 
        UUID memberId, 
        UpdateMemberRoleRequest request, 
        UUID requestingUserId
    ) {
        log.info("🔄 MAJ rôle membre {} du trip {}", memberId, tripId);

        return tripRepository.findById(tripId)
            .switchIfEmpty(Mono.error(new RuntimeException("Voyage non trouvé")))
            .flatMap(trip -> {
                if (!trip.isOwner(requestingUserId)) {
                    return Mono.error(new RuntimeException("Seul owner peut changer rôles"));
                }
                return Mono.just(trip);
            })
            .then(memberRepository.findByTripIdAndUserId(tripId, memberId))
            .switchIfEmpty(Mono.error(new RuntimeException("Membre non trouvé")))
            .flatMap(member -> {
                if (member.getRole() == TripMember.MemberRole.OWNER) {
                    return Mono.error(new RuntimeException("Impossible de modifier rôle OWNER"));
                }

                member.setRole(TripMember.MemberRole.valueOf(request.getNewRole()));
                return memberRepository.save(member);
            })
            .flatMap(member -> userRepository.findById(member.getUserId())
                .map(user -> TripMemberDTO.builder()
                    .userId(member.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(member.getRole().name())
                    .joinedAt(member.getJoinedAt())
                    .notificationsEnabled(member.getNotificationsEnabled())
                    .build()));
    }

    @Transactional
    public Mono<Void> removeMember(UUID tripId, UUID memberId, UUID requestingUserId) {
        log.warn("🚫 Retrait membre {} du trip {}", memberId, tripId);

        return tripRepository.findById(tripId)
            .switchIfEmpty(Mono.error(new RuntimeException("Voyage non trouvé")))
            .flatMap(trip -> {
                if (!trip.isOwner(requestingUserId) && !memberId.equals(requestingUserId)) {
                    return Mono.error(new RuntimeException("Permission refusée"));
                }
                return Mono.just(trip);
            })
            .then(memberRepository.findByTripIdAndUserId(tripId, memberId))
            .switchIfEmpty(Mono.error(new RuntimeException("Membre non trouvé")))
            .flatMap(member -> {
                if (member.getRole() == TripMember.MemberRole.OWNER) {
                    return Mono.error(new RuntimeException("Impossible de retirer OWNER"));
                }
                return memberRepository.delete(member);
            })
            .doOnSuccess(v -> log.info("✅ Membre {} retiré du trip {}", memberId, tripId));
    }
}