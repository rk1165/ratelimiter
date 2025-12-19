package com.ratelimiter.configuration;

import com.ratelimiter.utils.ApplicationConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Long> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        // Use String serializer for keys (e.g. "rate_limit:user:123"
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // Use Long serializer for values (token counts, timestamps)
        redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Long.class));

        // Configure hash serializers for consistency of keys and values i.e. String instead of binary
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericToStringSerializer<>(Long.class));

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public RedisScript<List<Long>> tokenBucketScript() {
        return (RedisScript<List<Long>>) (RedisScript<?>) RedisScript.of(ApplicationConstants.TOKEN_BUCKET_SCRIPT, List.class);
    }
}
