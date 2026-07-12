package com.auction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Loads the bid-validation Lua script from resources/scripts/bid_validation.lua.
     * This script is what makes bid validation ATOMIC: "is this bid higher than
     * the current one" and "is this bid a valid increment step" are checked and
     * applied in a single round-trip to Redis, so two teams bidding at the exact
     * same instant can never both be accepted.
     */
    @Bean
    public DefaultRedisScript<java.util.List> bidValidationScript() {
        DefaultRedisScript<java.util.List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/bid_validation.lua"));
        script.setResultType(java.util.List.class);
        return script;
    }
}
