package com.testexecutionplatform.repository;

import com.testexecutionplatform.model.TestPlanExecution;
import com.testexecutionplatform.model.TestPlanExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestPlanExecutionLogRepository extends JpaRepository<TestPlanExecutionLog, Long> {
    List<TestPlanExecutionLog> findByExecutionOrderByCreatedAtAsc(TestPlanExecution execution);
    List<TestPlanExecutionLog> findByExecutionIdOrderByCreatedAtAsc(Long executionId);
    Page<TestPlanExecutionLog> findByExecutionIdOrderByCreatedAtAsc(Long executionId, Pageable pageable);
}