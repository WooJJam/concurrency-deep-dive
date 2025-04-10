package co.kr.woojjam.concurrency.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import co.kr.woojjam.concurrency.entity.TestCoupon;
import jakarta.persistence.LockModeType;

public interface TestCouponRepository extends JpaRepository<TestCoupon, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from TestCoupon c where c.id = :id")
	Optional<TestCoupon> findByIdWithPessimisticWrite(Long id);

	@Lock(LockModeType.OPTIMISTIC)
	@Query("select c from TestCoupon c where c.id = :id")
	Optional<TestCoupon> findByWithOptimistic(Long id);



}
