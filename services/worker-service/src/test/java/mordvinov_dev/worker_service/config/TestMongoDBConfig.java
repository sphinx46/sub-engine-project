package mordvinov_dev.worker_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@TestConfiguration
public class TestMongoDBConfig {

    private static final MongoDBContainer mongodb = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0")
    );

    static {
        mongodb.start();
    }

    @Bean
    @Primary
    public MongoClient mongoClient() {
        return MongoClients.create(mongodb.getConnectionString());
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(
                mongoClient(),
                "test"
        ));
    }

    public static String getConnectionString() {
        return mongodb.getConnectionString() + "/test";
    }
}