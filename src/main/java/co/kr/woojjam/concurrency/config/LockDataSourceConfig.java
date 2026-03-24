package co.kr.woojjam.concurrency.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class LockDataSourceConfig {

	@Bean(name = "lockDataSource")
	@ConfigurationProperties(prefix = "spring.lock-datasource")
	public DataSource lockDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "lockJdbcTemplate")
	public JdbcTemplate lockJdbcTemplate(@Qualifier("lockDataSource") DataSource lockDataSource) {
		return new JdbcTemplate(lockDataSource);
	}
}
