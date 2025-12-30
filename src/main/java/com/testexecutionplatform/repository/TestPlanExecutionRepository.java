package com.testexecutionplatform.repository;

import com.testexecutionplatform.model.TestPlan;
import com.testexecutionplatform.model.TestPlanExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestPlanExecutionRepository extends JpaRepository<TestPlanExecution, Long> {
    List<TestPlanExecution> findByTestPlanOrderByCreatedAtDesc(TestPlan testPlan);
    
    @Query("SELECT e FROM TestPlanExecution e JOIN FETCH e.executionNode WHERE e.testPlan.id = :planId ORDER BY e.createdAt DESC")
    List<TestPlanExecution> findByTestPlanIdOrderByCreatedAtDesc(@Param("planId") Long planId);
}