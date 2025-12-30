package com.testexecutionplatform.service;

import com.testexecutionplatform.model.TestPlanExecution;
import com.testexecutionplatform.model.TestPlanExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TestPlanExecutionService {
    TestPlanExecution executeTestPlan(Long planId, List<Long> nodeIdList);
    TestPlanExecution getExecutionById(Long executionId);
    List<TestPlanExecution> getExecutionsByPlanId(Long planId);
    Page<TestPlanExecutionLog> getExecutionLogsByExecutionId(Long executionId, Pageable pageable);
    List<TestPlanExecutionLog> getExecutionLogsByExecutionId(Long executionId);
    TestPlanExecutionLog getExecutionLogById(Long logId); // 添加根据logId获取日志的方法
    void updateExecutionStatus(Long executionId, String status);
    void updateExecutionLogStatus(Long logId, String status, String result, String errorMessage, Long executionTime);
    TestPlanExecutionLog createExecutionLog(TestPlanExecutionLog log);
    TestPlanExecution updateExecution(TestPlanExecution execution);
}