package co.kr.woojjam.concurrency.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

import co.kr.woojjam.concurrency.common.lock.DistributedLockRepository;
import co.kr.woojjam.concurrency.common.lock.named.NamedLockOptions;
import co.kr.woojjam.concurrency.common.lock.named.NamedLockRepository;

@Configuration
public class LockDataSourceConfig {

	@Bean
	@ConfigurationProperties("spring.lock-datasource")
	public DataSourceProperties lockDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean(name = "lockDataSource")
	@ConfigurationProperties("spring.lock-datasource.hikari")
	public DataSource lockDataSource(
		@Qualifier("lockDataSourceProperties") DataSourceProperties lockDataSourceProperties
	) {
		return lockDataSourceProperties.initializeDataSourceBuilder()
			.type(HikariDataSource.class)
			.build();
	}

	@Bean
	public DistributedLockRepository<NamedLockOptions> lockRepository(
		@Qualifier("lockDataSource") DataSource lockDataSource
	) {
		return new NamedLockRepository(lockDataSource);
	}
}
