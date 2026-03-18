package mordvinov_dev.worker_service;

import mordvinov_dev.worker_service.config.TestMongoDBConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TestMongoDBConfig.class)
class WorkerServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
