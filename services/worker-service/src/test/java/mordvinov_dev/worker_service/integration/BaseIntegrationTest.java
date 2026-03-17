package mordvinov_dev.worker_service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import mordvinov_dev.worker_service.config.TestMongoDBConfig;
import mordvinov_dev.worker_service.config.TestSecurityConfig;
import mordvinov_dev.worker_service.config.TestKafkaConfig;
import mordvinov_dev.worker_service.config.TestNotificationConfig;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@ContextConfiguration(classes = {TestMongoDBConfig.class, TestSecurityConfig.class, TestKafkaConfig.class, TestNotificationConfig.class})
public abstract class BaseIntegrationTest {

    static {
        System.setProperty("MONGODB_TEST_URI", TestMongoDBConfig.getConnectionString());
    }
}