package com.enspy.tripplanning.notification.service;

import com.enspy.tripplanning.authentification.entity.User;
import com.enspy.tripplanning.authentification.repository.UserRepository;
import com.enspy.tripplanning.notification.entity.Notification;
import com.enspy.tripplanning.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Envoyer une notification à tous les utilisateurs après création d'un nouveau
     * POI
     */
    public Flux<Notification> sendNewPoiNotificationToAllUsers(Long poiId, String poiName) {
        log.info("📍 Notification nouveau POI '{}' pour tous les utilisateurs", poiName);

        return userRepository.findByRole(User.UserRole.USER)
                .flatMap(user -> {
                    Notification notif = Notification.builder()
                            .userId(user.getUserId())
                            .type("NEW_POI")
                            .title("Nouveau lieu disponible")
                            .message("Un nouveau point d'intérêt '" + poiName + "' a été ajouté")
                            .relatedEntityType("POI")
                            .relatedEntityId(poiId.toString())
                            .actionUrl("/pois/" + poiId)
                            .build();
                    return notificationRepository.save(notif);
                });
    }

    /**
     * Envoyer une notification à tous les admins après création d'un nouvel
     * utilisateur
     */
    public Flux<Notification> sendNewUserNotificationToAdmins(UUID newUserId, String newUserName) {
        log.info("👤 Notification nouvel utilisateur '{}' pour les admins", newUserName);

        return userRepository.findByRole(User.UserRole.ADMIN)
                .flatMap(admin -> {
                    Notification notif = Notification.builder()
                            .userId(admin.getUserId())
                            .type("NEW_USER")
                            .title("Nouveau utilisateur inscrit")
                            .message("Un nouvel utilisateur '" + newUserName + "' vient de s'inscrire")
                            .relatedEntityType("USER")
                            .relatedEntityId(newUserId.toString())
                            .actionUrl("/admin/users/" + newUserId)
                            .build();
                    return notificationRepository.save(notif);
                });
    }

    public Mono<Notification> sendTripInviteNotification(UUID invitedUserId, UUID tripId, String inviterName) {
        log.info("📧 Notification invitation trip {} pour user {}", tripId, invitedUserId);

        Notification notif = Notification.builder()
                .userId(invitedUserId)
                .type("TRIP_INVITE")
                .title("Invitation voyage")
                .message(inviterName + " vous a invité à collaborer sur un voyage")
                .relatedEntityType("TRIP")
                .relatedEntityId(tripId.toString())
                .actionUrl("/trips/" + tripId)
                .build();

        return notificationRepository.save(notif);
    }

    public Mono<Notification> sendPoiVerifiedNotification(UUID userId, Long poiId, String poiName) {
        Notification notif = Notification.builder()
                .userId(userId)
                .type("POI_VERIFIED")
                .title("POI vérifié")
                .message("Votre POI '" + poiName + "' a été vérifié par un administrateur")
                .relatedEntityType("POI")
                .relatedEntityId(poiId.toString())
                .actionUrl("/pois/" + poiId)
                .build();

        return notificationRepository.save(notif);
    }

    public Flux<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId)
                .sort(Comparator.comparing(Notification::getCreatedAt).reversed());
    }

    public Flux<Notification> getAllNotifications(UUID userId) {
        return notificationRepository.findByUserId(userId)
                .sort(Comparator.comparing(Notification::getCreatedAt).reversed());
    }

    public Mono<Notification> markAsRead(UUID notificationId, UUID userId) {
        return notificationRepository.findById(notificationId)
                .filter(notif -> notif.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Notification non trouvée")))
                .map(notif -> {
                    notif.markAsRead();
                    return notif;
                })
                .flatMap(notificationRepository::save);
    }

    public Mono<Void> markAllAsRead(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId)
                .flatMap(notif -> {
                    notif.markAsRead();
                    return notificationRepository.save(notif);
                })
                .then();
    }

    public Mono<Void> deleteNotification(UUID notificationId, UUID userId) {
        return notificationRepository.findById(notificationId)
                .filter(notif -> notif.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Notification non trouvée")))
                .flatMap(notificationRepository::delete);
    }

    public Mono<Long> countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Envoyer une notification de bienvenue à un nouvel utilisateur
     */
    public Mono<Notification> sendWelcomeNotification(UUID userId, String userName) {
        log.info("🎉 Notification de bienvenue pour '{}'", userName);

        Notification notif = Notification.builder()
                .userId(userId)
                .type("WELCOME")
                .title("Bienvenue sur Planning Map!")
                .message("Bonjour " + userName
                        + ", bienvenue sur Planning Map! Explorez les points d'intérêt et planifiez vos itinéraires.")
                .relatedEntityType("USER")
                .relatedEntityId(userId.toString())
                .actionUrl("/dashboard")
                .build();

        return notificationRepository.save(notif);
    }

    /**
     * Envoyer une notification à tous les admins lorsqu'un utilisateur supprime son
     * compte
     */
    public Flux<Notification> sendUserDeletedNotificationToAdmins(UUID deletedUserId, String userName) {
        log.info("🗑️ Notification suppression compte '{}' pour les admins", userName);

        return userRepository.findByRole(User.UserRole.ADMIN)
                .flatMap(admin -> {
                    Notification notif = Notification.builder()
                            .userId(admin.getUserId())
                            .type("USER_DELETED")
                            .title("Compte utilisateur supprimé")
                            .message("L'utilisateur '" + userName + "' a supprimé son compte")
                            .relatedEntityType("USER")
                            .relatedEntityId(deletedUserId.toString())
                            .actionUrl("/admin/users")
                            .build();
                    return notificationRepository.save(notif);
                });
    }

    /**
     * Envoyer une notification à tous les admins lorsqu'un commentaire est fait sur
     * un POI
     */
    public Flux<Notification> sendPoiCommentNotificationToAdmins(Long poiId, String poiName, String commenterName,
            String commentText) {
        log.info("💬 Notification commentaire POI '{}' par '{}' pour les admins", poiName, commenterName);

        // Tronquer le commentaire s'il est trop long
        String truncatedComment = commentText != null && commentText.length() > 100
                ? commentText.substring(0, 100) + "..."
                : commentText;

        return userRepository.findByRole(User.UserRole.ADMIN)
                .flatMap(admin -> {
                    Notification notif = Notification.builder()
                            .userId(admin.getUserId())
                            .type("POI_COMMENT")
                            .title("Nouveau commentaire sur '" + poiName + "'")
                            .message(commenterName + ": \"" + truncatedComment + "\"")
                            .relatedEntityType("POI")
                            .relatedEntityId(poiId.toString())
                            .actionUrl("/Admin/PoiManagement/" + poiId + "#comments")
                            .build();
                    return notificationRepository.save(notif);
                });
    }

    /**
     * Envoyer une notification à tous les utilisateurs lorsqu'un admin modifie un
     * POI
     */
    public Flux<Notification> sendPoiEditedNotificationToUsers(Long poiId, String poiName) {
        log.info("✏️ Notification édition POI '{}' pour tous les utilisateurs", poiName);

        return userRepository.findByRole(User.UserRole.USER)
                .flatMap(user -> {
                    Notification notif = Notification.builder()
                            .userId(user.getUserId())
                            .type("POI_EDITED")
                            .title("Lieu mis à jour")
                            .message("Le point d'intérêt '" + poiName + "' a été mis à jour")
                            .relatedEntityType("POI")
                            .relatedEntityId(poiId.toString())
                            .actionUrl("/pois/" + poiId)
                            .build();
                    return notificationRepository.save(notif);
                });
    }

    /**
     * Envoyer une notification aux utilisateurs qui ont un POI en favori lorsqu'il
     * est supprimé
     */
    public Flux<Notification> sendPoiDeletedNotificationToFavorites(Long poiId, String poiName, Flux<UUID> userIds) {
        log.info("🗑️ Notification suppression POI '{}' aux favoris", poiName);

        return userIds.flatMap(userId -> {
            Notification notif = Notification.builder()
                    .userId(userId)
                    .type("POI_DELETED")
                    .title("Lieu supprimé")
                    .message("Le point d'intérêt '" + poiName + "' que vous aviez en favori a été supprimé")
                    .relatedEntityType("POI")
                    .relatedEntityId(poiId.toString())
                    .actionUrl("/favorites")
                    .build();
            return notificationRepository.save(notif);
        });
    }

    /**
     * Envoyer une notification aux utilisateurs qui ont un POI en favori lorsqu'il
     * y a une activité dessus
     */
    public Flux<Notification> sendFavoriteActivityNotification(Long poiId, String poiName, String actorName,
            String activityType, Flux<UUID> favoriteUserIds, UUID actorId, String commentText) {
        log.info("⭐ Notification activité '{}' sur POI '{}' par '{}'", activityType, poiName, actorName);

        String title;
        String message;
        if (activityType.equals("LIKE")) {
            title = "Quelqu'un a aimé '" + poiName + "'";
            message = actorName + " a aimé le lieu '" + poiName + "'";
        } else {
            // Inclure le texte du commentaire pour les notifications COMMENT
            title = "Commentaire sur '" + poiName + "'";
            String truncatedComment = commentText != null && commentText.length() > 100
                    ? commentText.substring(0, 100) + "..."
                    : commentText;
            message = actorName + ": \"" + truncatedComment + "\"";
        }

        return favoriteUserIds
                .filter(userId -> !userId.equals(actorId)) // Ne pas notifier l'auteur de l'action
                .flatMap(userId -> {
                    Notification notif = Notification.builder()
                            .userId(userId)
                            .type("FAVORITE_ACTIVITY")
                            .title(title)
                            .message(message)
                            .relatedEntityType("POI")
                            .relatedEntityId(poiId.toString())
                            .actionUrl("/pois/" + poiId)
                            .build();
                    return notificationRepository.save(notif);
                });
    }

    /**
     * Envoyer une notification à tous les utilisateurs lorsqu'un POI est supprimé
     */
    public Flux<Notification> sendPoiDeletedNotificationToAllUsers(Long poiId, String poiName) {
        log.info("🗑️ Notification suppression POI '{}' pour tous les utilisateurs", poiName);

        return userRepository.findByRole(User.UserRole.USER)
                .flatMap(user -> {
                    Notification notif = Notification.builder()
                            .userId(user.getUserId())
                            .type("POI_DELETED")
                            .title("Lieu supprimé")
                            .message("Le point d'intérêt '" + poiName + "' a été supprimé")
                            .relatedEntityType("POI")
                            .relatedEntityId(poiId.toString())
                            .actionUrl("/")
                            .build();
                    return notificationRepository.save(notif);
                });
    }
}