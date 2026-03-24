package co.kr.woojjam.concurrency.repository.match;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
public class MatchLockRepository {

	private final JdbcTemplate lockJdbcTemplate;

	public MatchLockRepository(@Qualifier("lockJdbcTemplate") JdbcTemplate lockJdbcTemplate) {
		this.lockJdbcTemplate = lockJdbcTemplate;
	}

	public Integer getLock(String lockName, int timeout) {
		return lockJdbcTemplate.queryForObject(
			"SELECT GET_LOCK(?, ?)", Integer.class, lockName, timeout
		);
	}

	public Integer releaseLock(String lockName) {
		return lockJdbcTemplate.queryForObject(
			"SELECT RELEASE_LOCK(?)", Integer.class, lockName
		);
	}
}
