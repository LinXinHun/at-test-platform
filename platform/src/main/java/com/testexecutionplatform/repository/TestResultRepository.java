package com.testexecutionplatform.repository;

import com.testexecutionplatform.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    List<TestResult> findByTaskId(Long taskId);
}
