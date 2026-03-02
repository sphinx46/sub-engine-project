package mordvinov_dev.sub_engine_project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SubEngineProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
