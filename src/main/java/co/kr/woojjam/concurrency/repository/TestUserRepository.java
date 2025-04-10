package co.kr.woojjam.concurrency.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.kr.woojjam.concurrency.entity.TestUser;

public interface TestUserRepository extends JpaRepository<TestUser, Long> {
}
