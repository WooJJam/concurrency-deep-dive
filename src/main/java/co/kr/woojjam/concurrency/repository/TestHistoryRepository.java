package co.kr.woojjam.concurrency.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.kr.woojjam.concurrency.entity.TestHistory;

public interface TestHistoryRepository extends JpaRepository<TestHistory, Long> {
}
