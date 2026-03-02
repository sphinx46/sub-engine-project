package mordvinov_dev.sub_engine_project;

import org.springframework.boot.SpringApplication;

public class TestSubEngineProjectApplication {

	public static void main(String[] args) {
		SpringApplication.from(SubEngineProjectApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
