package co.kr.woojjam.concurrency.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import co.kr.woojjam.concurrency.entity.TestCoupon;
import co.kr.woojjam.concurrency.entity.TestUser;
import jakarta.persistence.LockModeType;

public interface TestUserRepository extends JpaRepository<TestUser, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from TestUser u where u.id = :id")
	Optional<TestUser> findByIdWithPessimisticWrite(Long id);
}
