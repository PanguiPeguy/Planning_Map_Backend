package com.enspy.tripplanning.authentification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.enspy.tripplanning.authentification.dto.RegisterRequest;
import com.enspy.tripplanning.authentification.entity.User;
import com.enspy.tripplanning.authentification.repository.UserRepository;
import com.enspy.tripplanning.notification.service.NotificationService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public Mono<User> register(RegisterRequest req) {
        var user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .companyName(req.getCompanyName())
                .phone(req.getPhone())
                .city(req.getCity())
                .transportmode(req.getTransportmode())
                .profilePhotoUrl(req.getProfilePhotoUrl())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();

        return userRepository.save(user)
                .doOnSuccess(savedUser -> {
                    // Envoyer notification aux admins
                    notificationService.sendNewUserNotificationToAdmins(
                            savedUser.getUserId(),
                            savedUser.getDisplayName())
                            .subscribe();
                    // Envoyer notification de bienvenue à l'utilisateur
                    notificationService.sendWelcomeNotification(
                            savedUser.getUserId(),
                            savedUser.getDisplayName())
                            .subscribe();
                });
    }

    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Mono<com.enspy.tripplanning.authentification.dto.UserProfileResponse> getUserProfile(java.util.UUID userId) {
        return userRepository.findById(userId)
                .map(user -> com.enspy.tripplanning.authentification.dto.UserProfileResponse.builder()
                        .id(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .companyName(user.getCompanyName())
                        .phone(user.getPhone())
                        .city(user.getCity())
                        .transportmode(user.getTransportmode())
                        .profilePhotoUrl(user.getProfilePhotoUrl())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build());
    }

    public Mono<com.enspy.tripplanning.authentification.dto.UserProfileResponse> updateProfile(java.util.UUID userId,
            com.enspy.tripplanning.authentification.dto.UpdateProfileRequest request) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    if (request.getUsername() != null)
                        user.setUsername(request.getUsername());
                    if (request.getCompanyName() != null)
                        user.setCompanyName(request.getCompanyName());
                    if (request.getPhone() != null)
                        user.setPhone(request.getPhone());
                    if (request.getCity() != null)
                        user.setCity(request.getCity());
                    if (request.getTransportmode() != null)
                        user.setTransportmode(request.getTransportmode());
                    return userRepository.save(user);
                })
                .map(user -> com.enspy.tripplanning.authentification.dto.UserProfileResponse.builder()
                        .id(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .companyName(user.getCompanyName())
                        .phone(user.getPhone())
                        .city(user.getCity())
                        .transportmode(user.getTransportmode())
                        .profilePhotoUrl(user.getProfilePhotoUrl())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build());
    }

    public Flux<com.enspy.tripplanning.authentification.dto.UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .map(user -> com.enspy.tripplanning.authentification.dto.UserProfileResponse.builder()
                        .id(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .companyName(user.getCompanyName())
                        .phone(user.getPhone())
                        .city(user.getCity())
                        .transportmode(user.getTransportmode())
                        .profilePhotoUrl(user.getProfilePhotoUrl())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build());
    }

    public Mono<Void> deleteUser(java.util.UUID userId) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    String userName = user.getDisplayName();
                    return userRepository.deleteById(userId)
                            .doOnSuccess(v -> {
                                // Envoyer notification aux admins
                                notificationService.sendUserDeletedNotificationToAdmins(
                                        userId,
                                        userName)
                                        .subscribe();
                            });
                })
                .switchIfEmpty(userRepository.deleteById(userId));
    }
}