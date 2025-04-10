package co.kr.woojjam.concurrency.config;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

@Testcontainers
@ActiveProfiles("test")
public abstract class TestDataBaseConfig {
	private static final String MYSQL_CONTAINER_IMAGE = "mysql:8.0.35";
	private static final MySQLContainer MYSQL_CONTAINER;

	private static final String REDIS_CONTAINER_IMAGE = "redis:7.2.4-alpine";
	private static final RedisContainer REDIS_CONTAINER;

	static {
		MYSQL_CONTAINER = new MySQLContainer(DockerImageName.parse(MYSQL_CONTAINER_IMAGE));
		REDIS_CONTAINER = new RedisContainer(DockerImageName.parse(REDIS_CONTAINER_IMAGE));
		MYSQL_CONTAINER.start();
		REDIS_CONTAINER.start();
	}

	@DynamicPropertySource
	public static void setProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
		registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
		registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
		registry.add("spring.data.redis.port",
			() -> String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));
	}
}
