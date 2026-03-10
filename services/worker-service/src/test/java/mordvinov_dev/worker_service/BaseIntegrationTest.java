package mordvinov_dev.worker_service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import mordvinov_dev.worker_service.config.TestMongoDBConfig;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@ContextConfiguration(classes = TestMongoDBConfig.class)
public abstract class BaseIntegrationTest {

    static {
        System.setProperty("MONGODB_TEST_URI", TestMongoDBConfig.getConnectionString());
    }
}