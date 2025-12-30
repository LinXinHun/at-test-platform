package com.testexecutionplatform.service;

import com.testexecutionplatform.model.TestExecutionTask;

import java.util.List;

public interface TestExecutionTaskService {
    List<TestExecutionTask> getAllTestExecutionTasks();
    TestExecutionTask getTestExecutionTaskById(Long id);
    TestExecutionTask createTestExecutionTask(TestExecutionTask testExecutionTask);
    TestExecutionTask updateTestExecutionTask(Long id, TestExecutionTask testExecutionTask);
    void deleteTestExecutionTask(Long id);
    TestExecutionTask startTask(Long id);
    TestExecutionTask stopTask(Long id);
    TestExecutionTask completeTask(Long id); // 新增方法：完成任务
    List<TestExecutionTask> getTasksByPlanId(Long planId);
}