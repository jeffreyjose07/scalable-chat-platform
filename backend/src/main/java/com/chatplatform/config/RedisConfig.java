package com.chatplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

@Configuration
public class RedisConfig {

    // Standard non-TLS Redis port — Upstash and most managed providers use 6380 for TLS
    private static final int REDIS_PLAINTEXT_PORT = 6379;
    private static final int REDIS_TLS_PORT = 6380;

    @Value("${spring.redis.url:redis://localhost:6379}")
    private String redisUrl;

    // Honoured as a fallback so that 'spring.redis.ssl: true' in application-render.yml
    // takes effect even when REDIS_URL still carries the plain redis:// scheme.
    @Value("${spring.redis.ssl:false}")
    private boolean sslEnabled;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        URI uri = URI.create(redisUrl);

        boolean useSSL = "rediss".equals(uri.getScheme()) || sslEnabled;

        // When SSL is required but the URL still points at the standard plaintext port
        // (common misconfiguration with Upstash where TLS lives on 6380), correct it.
        int port = uri.getPort() > 0 ? uri.getPort() : REDIS_PLAINTEXT_PORT;
        if (useSSL && port == REDIS_PLAINTEXT_PORT) {
            port = REDIS_TLS_PORT;
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(uri.getHost());
        config.setPort(port);

        if (uri.getUserInfo() != null) {
            // Split only on the first colon so passwords containing ':' are handled correctly.
            int colonIdx = uri.getUserInfo().indexOf(':');
            if (colonIdx > 0) {
                config.setUsername(uri.getUserInfo().substring(0, colonIdx));
                config.setPassword(uri.getUserInfo().substring(colonIdx + 1));
            }
        }

        LettuceClientConfiguration clientConfig = useSSL
                ? LettuceClientConfiguration.builder().useSsl().build()
                : LettuceClientConfiguration.defaultConfiguration();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
