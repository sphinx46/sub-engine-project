package mordvinov_dev.billing_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder mockJwtDecoder() {
        return org.mockito.Mockito.mock(JwtDecoder.class);
    }
}
