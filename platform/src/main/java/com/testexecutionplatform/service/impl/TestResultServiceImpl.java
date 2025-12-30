package com.testexecutionplatform.service.impl;

import com.testexecutionplatform.model.TestExecutionTask;
import com.testexecutionplatform.model.TestReport;
import com.testexecutionplatform.model.TestResult;
import com.testexecutionplatform.repository.TestResultRepository;
import com.testexecutionplatform.service.TestExecutionTaskService;
import com.testexecutionplatform.service.TestReportService;
import com.testexecutionplatform.service.TestResultService;
import com.testexecutionplatform.repository.TestReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestResultServiceImpl implements TestResultService {

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private TestExecutionTaskService testExecutionTaskService;

    @Autowired
    private TestReportService testReportService;

    @Autowired
    private TestReportRepository testReportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<TestResult> getAllTestResults() {
        return testResultRepository.findAll();
    }

    @Override
    public TestResult getTestResultById(Long id) {
        Optional<TestResult> optionalTestResult = testResultRepository.findById(id);
        return optionalTestResult.orElse(null);
    }

    @Override
    public TestResult createTestResult(TestResult testResult) {
        TestResult savedResult = testResultRepository.save(testResult);
        
        // 检查任务是否完成，若完成则生成测试报告
        TestExecutionTask task = testExecutionTaskService.getTestExecutionTaskById(testResult.getTask().getId());
        List<TestResult> taskResults = testResultRepository.findByTaskId(task.getId());
        
        // 简单判断：如果任务已经有测试结果，并且任务状态是COMPLETED，则生成报告
        if (task.getStatus().equals("COMPLETED") && !taskResults.isEmpty()) {
            generateTestReport(task, taskResults);
        }
        
        return savedResult;
    }

    @Override
    public TestResult updateTestResult(Long id, TestResult testResult) {
        if (testResultRepository.existsById(id)) {
            testResult.setId(id);
            TestResult updatedResult = testResultRepository.save(testResult);
            
            // 更新后也检查是否需要生成报告
            TestExecutionTask task = testExecutionTaskService.getTestExecutionTaskById(updatedResult.getTask().getId());
            List<TestResult> taskResults = testResultRepository.findByTaskId(task.getId());
            
            if (task.getStatus().equals("COMPLETED") && !taskResults.isEmpty()) {
                generateTestReport(task, taskResults);
            }
            
            return updatedResult;
        }
        return null;
    }

    @Override
    public void deleteTestResult(Long id) {
        testResultRepository.deleteById(id);
    }

    @Override
    public List<TestResult> getTestResultsByTaskId(Long taskId) {
        return testResultRepository.findByTaskId(taskId);
    }

    /**
     * 生成测试报告
     */
    private void generateTestReport(TestExecutionTask task, List<TestResult> results) {
        // 统计测试结果
        int totalCount = results.size();
        int passedCount = (int) results.stream().filter(result -> result.getStatus().equals("SUCCESS")).count();
        int failedCount = totalCount - passedCount;
        double successRate = totalCount > 0 ? (double) passedCount / totalCount * 100 : 0;
        
        // 生成报告数据（JSON格式）
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("totalScripts", totalCount);
        reportData.put("successScripts", passedCount);
        reportData.put("failedScripts", failedCount);
        reportData.put("passRate", successRate);

        // 添加脚本执行详情
        List<Map<String, Object>> scriptDetails = results.stream().map(result -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("scriptName", result.getScript().getName());
            detail.put("status", result.getStatus());
            detail.put("executionTime", result.getDuration());
            detail.put("errorMessage", result.getError());
            return detail;
        }).collect(java.util.stream.Collectors.toList());
        reportData.put("scriptDetails", scriptDetails);
        
        // 创建测试报告
        TestReport report = new TestReport();
        // 设置为null，因为TestExecutionTask没有对应的execution
        report.setExecutionId(null);
        report.setPlanName(task.getPlan().getName());
        report.setTotalScripts(totalCount);
        report.setSuccessScripts(passedCount);
        report.setFailedScripts(failedCount);
        report.setPassRate(successRate);
        report.setGenerationTime(LocalDateTime.now());
        report.setStatus(failedCount > 0 ? "FAILED" : "SUCCESS");
        
        // 设置报告数据（JSON格式）
        try {
            report.setReportData(objectMapper.writeValueAsString(reportData));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("生成报告数据失败: " + e.getMessage());
        }
        
        // 保存测试报告
        testReportRepository.save(report);
    }

    /**
     * 生成报告摘要（已废弃，保留仅用于兼容性）
     */
    private String generateSummary(List<TestResult> results) {
        StringBuilder summary = new StringBuilder();
        summary.append("测试结果摘要:\n\n");
        
        // 添加每个测试结果的详细信息
        for (TestResult result : results) {
            summary.append("测试项: ").append(result.getScript().getName()).append("\n");
            summary.append("状态: ").append(result.getStatus()).append("\n");
            if (result.getDuration() != null) {
                summary.append("耗时: ").append(result.getDuration()).append("ms\n");
            }
            if (result.getError() != null && !result.getError().isEmpty()) {
                summary.append("错误信息: ").append(result.getError()).append("\n");
            }
            summary.append("\n");
        }
        
        return summary.toString();
    }
}