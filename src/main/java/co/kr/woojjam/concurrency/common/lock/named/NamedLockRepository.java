package co.kr.woojjam.concurrency.common.lock.named;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.sql.DataSource;

import co.kr.woojjam.concurrency.common.lock.DistributedLockRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NamedLockRepository implements DistributedLockRepository<NamedLockOptions> {

	private static final String GET_LOCK_QUERY = "SELECT GET_LOCK(?, ?)";
	private static final String RELEASE_LOCK_QUERY = "SELECT RELEASE_LOCK(?)";

	private final DataSource dataSource;

	public NamedLockRepository(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void withLock(final NamedLockOptions options, final Runnable action) {

		try (final Connection connection = dataSource.getConnection()) {
			try {
				getLock(connection, options.key(), options.timeoutSeconds());
				action.run();
			} finally {
				releaseLock(connection, options.key());
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private void getLock(final Connection connection, final String key, final Integer timeout) {

		try (PreparedStatement statement = connection.prepareStatement(GET_LOCK_QUERY)) {
			statement.setString(1, key);
			statement.setInt(2, timeout);
			checkResult(statement, LockType.GET, key);
		} catch (SQLException e) {
			log.error("Failed to get lock : {}", key, e);
			throw new RuntimeException("Named Lock 을 얻는 중 오류가 발생하였습니다.");
		}
	}

	private void releaseLock(final Connection connection, final String key) {

		try (PreparedStatement statement = connection.prepareStatement(RELEASE_LOCK_QUERY)) {
			statement.setString(1, key);
			checkResult(statement, LockType.RELEASE, key);
		} catch (SQLException e) {
			log.error("Failed to release lock : {}", key, e);
			throw new RuntimeException("Named Lock 을 해제하는 중 오류가 발생하였습니다.");
		}
	}

	private void checkResult(
		final PreparedStatement statement,
		final LockType lockType,
		final String... keys
		) {
		try (final ResultSet resultSet = statement.executeQuery()) {
			if (!resultSet.next()) {
				log.error("Lock {} 실패 : {}", lockType.name(), Arrays.toString(keys));
				throw new RuntimeException("Lock 실패 " + lockType.name());
			}

			int result = resultSet.getInt(1);

			if (result == 0) {
				log.error("Lock {} 실패 : {}", lockType.name(), Arrays.toString(keys));
				throw new IllegalStateException("에러 발생" + keys[0]);
			}

			log.info("Lock {} 성공 : {}", lockType.name(), Arrays.toString(keys));
		} catch (SQLException e) {
			throw new RuntimeException("Lock 결과 조회 중 오류", e);
		}
	}

	enum LockType {
		GET, RELEASE, RELEASE_ALL
	}
}
