package com.testexecutionplatform.service;

import com.testexecutionplatform.model.TestResult;

import java.util.List;

public interface TestResultService {
    List<TestResult> getAllTestResults();
    TestResult getTestResultById(Long id);
    TestResult createTestResult(TestResult testResult);
    TestResult updateTestResult(Long id, TestResult testResult);
    void deleteTestResult(Long id);
    List<TestResult> getTestResultsByTaskId(Long taskId);
}
