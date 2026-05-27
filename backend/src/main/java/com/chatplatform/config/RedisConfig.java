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

    private static final int REDIS_PLAINTEXT_PORT = 6379;

    @Value("${spring.redis.url:redis://localhost:6379}")
    private String redisUrl;

    // Honoured as a fallback so that 'spring.redis.ssl: true' in application-render.yml
    // takes effect even when REDIS_URL still carries the plain redis:// scheme.
    @Value("${spring.redis.ssl:false}")
    private boolean sslEnabled;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        URI uri = URI.create(redisUrl);

        // Upstash (and Redis Cloud) serve TLS on port 6379 via the rediss:// scheme.
        // The ssl config property is a fallback for when REDIS_URL carries redis:// by mistake.
        boolean useSSL = "rediss".equals(uri.getScheme()) || sslEnabled;

        int port = uri.getPort() > 0 ? uri.getPort() : REDIS_PLAINTEXT_PORT;

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
