package com.testexecutionplatform.repository;

import com.testexecutionplatform.model.TestExecutionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestExecutionTaskRepository extends JpaRepository<TestExecutionTask, Long> {
    // 根据测试计划ID查询任务
    List<TestExecutionTask> findByPlanId(Long planId);
}
