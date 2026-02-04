package com.enspy.tripplanning.authentification.repository;

import com.enspy.tripplanning.authentification.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {
    Mono<User> findByEmail(String email);

    @Query("SELECT * FROM users WHERE role = :#{#role.name()}")
    Flux<User> findByRole(User.UserRole role);
}
