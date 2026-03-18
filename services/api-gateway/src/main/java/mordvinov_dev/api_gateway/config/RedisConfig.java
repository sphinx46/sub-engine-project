package mordvinov_dev.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Redis configuration for the API Gateway.
 * Configures both synchronous and reactive Redis connection factories
 * using Lettuce client with configurable host and port settings.
 */
@Configuration
public class RedisConfig {

    /**
     * Redis server host name.
     * Defaults to "redis" if not configured.
     */
    @Value("${spring.data.redis.host:redis}")
    private String redisHost;

    /**
     * Redis server port number.
     * Defaults to 6379 if not configured.
     */
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Creates and configures the primary Redis connection factory.
     * This factory is used for synchronous Redis operations and is marked as primary.
     * 
     * @return a configured Redis connection factory
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisHost, redisPort);
        factory.setValidateConnection(true);
        factory.setShareNativeConnection(false);
        return factory;
    }

    /**
     * Creates and configures a reactive Redis connection factory.
     * This factory is used for reactive Redis operations with WebFlux.
     * 
     * @return a configured reactive Redis connection factory
     */
    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisHost, redisPort);
        factory.setValidateConnection(true);
        factory.setShareNativeConnection(false);
        factory.afterPropertiesSet();
        return factory;
    }
}