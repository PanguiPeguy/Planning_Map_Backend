package com.enspy.tripplanning.authentification.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

public class ReactiveJwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveUserDetailsServiceImpl userDetailsService;

    public ReactiveJwtAuthenticationManager(JwtTokenProvider jwtTokenProvider,
            ReactiveUserDetailsServiceImpl userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        if (!jwtTokenProvider.validateToken(token)) {
            return Mono.empty();
        }

        String email = jwtTokenProvider.getEmailFromToken(token);

        return userDetailsService.findByUsername(email)
                .map(userDetails -> new UsernamePasswordAuthenticationToken(
                        userDetails,
                        token,
                        userDetails.getAuthorities()));
    }
}