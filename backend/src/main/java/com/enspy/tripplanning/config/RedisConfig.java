package com.enspy.tripplanning.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration Redis pour cache réactif.
 * 
 * Utilise Lettuce comme client Redis réactif avec sérialisation JSON
 * pour les POIs et routes calculées.
 * 
 * @author Planning Map Team
 * @version 1.0
 */
@Configuration
public class RedisConfig {

    /**
     * Template Redis réactif avec sérialisation JSON.
     * 
     * Configuration:
     * - Clés: String (ex: "poi:123", "route:hash")
     * - Valeurs: JSON (sérialisation Jackson)
     * - Support des types Java 8 Time (LocalDateTime, etc.)
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        // ObjectMapper avec support Java 8 Time
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Sérializer JSON pour les valeurs
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(objectMapper);

        // Sérializer String pour les clés
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // Context de sérialisation
        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .key(keySerializer)
                .value(serializer)
                .hashKey(keySerializer)
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
