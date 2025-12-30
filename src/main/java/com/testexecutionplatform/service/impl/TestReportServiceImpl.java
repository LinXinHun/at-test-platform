package com.testexecutionplatform.service.impl;

import com.testexecutionplatform.model.TestPlan;
import com.testexecutionplatform.model.TestPlanExecution;
import com.testexecutionplatform.model.TestPlanExecutionLog;
import com.testexecutionplatform.model.TestReport;
import com.testexecutionplatform.repository.TestPlanExecutionLogRepository;
import com.testexecutionplatform.repository.TestPlanExecutionRepository;
import com.testexecutionplatform.repository.TestPlanRepository;
import com.testexecutionplatform.repository.TestReportRepository;
import com.testexecutionplatform.service.TestReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TestReportServiceImpl implements TestReportService {

    private static final Logger logger = LoggerFactory.getLogger(TestReportServiceImpl.class);

    @Autowired
    private TestReportRepository testReportRepository;

    @Autowired
    private TestPlanExecutionRepository testPlanExecutionRepository;

    @Autowired
    private TestPlanExecutionLogRepository testPlanExecutionLogRepository;

    @Autowired
    private TestPlanRepository testPlanRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public TestReport generateReport(Long executionId) {
        logger.info("开始生成报告，执行记录ID: {}", executionId);
        
        // 根据执行记录ID获取执行记录
        TestPlanExecution execution = testPlanExecutionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在: " + executionId));

        // 获取执行记录的所有日志
        List<TestPlanExecutionLog> logs = testPlanExecutionLogRepository.findByExecutionIdOrderByCreatedAtAsc(executionId);
        logger.info("找到 {} 条执行日志", logs.size());

        // 计算测试结果统计数据
        int totalScripts = logs.size();
        int successScripts = (int) logs.stream().filter(log -> "SUCCESS".equals(log.getStatus())).count();
        int failedScripts = totalScripts - successScripts;
        double passRate = totalScripts > 0 ? (double) successScripts / totalScripts * 100 : 0;

        // 获取测试计划
        TestPlan testPlan = execution.getTestPlan();
        if (testPlan == null) {
            throw new RuntimeException("测试计划不存在");
        }
        logger.info("测试计划: {}, ID: {}", testPlan.getName(), testPlan.getId());

        // 查找上一次执行记录（排除当前执行记录）
        List<TestPlanExecution> previousExecutions = testPlanExecutionRepository
                .findByTestPlanIdOrderByCreatedAtDesc(testPlan.getId())
                .stream()
                .filter(e -> !e.getId().equals(executionId))
                .collect(Collectors.toList());
        
        logger.info("找到 {} 条上一次执行记录", previousExecutions.size());

        // 获取上一次执行记录的日志
        final Map<Long, TestPlanExecutionLog> previousLogsMap;
        if (!previousExecutions.isEmpty()) {
            TestPlanExecution previousExecution = previousExecutions.get(0);
            logger.info("上一次执行记录ID: {}", previousExecution.getId());
            List<TestPlanExecutionLog> previousLogs = 
                    testPlanExecutionLogRepository.findByExecutionIdOrderByCreatedAtAsc(previousExecution.getId());
            logger.info("上一次执行记录找到 {} 条日志", previousLogs.size());
            previousLogsMap = previousLogs.stream()
                    .collect(Collectors.toMap(
                            log -> log.getTestScript().getId(),
                            log -> log,
                            (existing, replacement) -> existing
                    ));
        } else {
            previousLogsMap = new HashMap<>();
        }

        // 生成报告数据（JSON格式）
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("totalScripts", totalScripts);
        reportData.put("successScripts", successScripts);
        reportData.put("failedScripts", failedScripts);
        reportData.put("passRate", passRate);
        reportData.put("hasPreviousExecution", !previousExecutions.isEmpty());
        if (!previousExecutions.isEmpty()) {
            reportData.put("previousExecutionId", previousExecutions.get(0).getId());
        }

        // 添加脚本执行详情和耗时对比
        List<Map<String, Object>> scriptDetails = logs.stream().map(log -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("scriptName", log.getTestScript().getName());
            detail.put("scriptId", log.getTestScript().getId());
            detail.put("status", log.getStatus());
            detail.put("executionTime", log.getExecutionTime());
            detail.put("errorMessage", log.getErrorMessage());
            
            // 添加耗时对比数据
            addTimeComparisonData(detail, log, previousLogsMap);
            
            return detail;
        }).collect(Collectors.toList());
        reportData.put("scriptDetails", scriptDetails);

        logger.info("报告数据生成完成，hasPreviousExecution: {}, 脚本详情数量: {}", 
            reportData.get("hasPreviousExecution"), scriptDetails.size());

        // 创建测试报告
        TestReport report = new TestReport();
        report.setExecution(execution);
        report.setPlanName(testPlan.getName());
        report.setTotalScripts(totalScripts);
        report.setSuccessScripts(successScripts);
        report.setFailedScripts(failedScripts);
        report.setPassRate(passRate);
        report.setGenerationTime(LocalDateTime.now());
        report.setReportData(mapToJson(reportData));
        report.setStatus("COMPLETED");

        // 保存报告到数据库
        TestReport savedReport = testReportRepository.save(report);
        logger.info("报告保存成功，报告ID: {}", savedReport.getId());
        
        return savedReport;
    }

    @Override
    public Optional<TestReport> getReportById(Long id) {
        return testReportRepository.findById(id);
    }

    @Override
    public Optional<TestReport> getReportByExecutionId(Long executionId) {
        return testReportRepository.findByExecutionId(executionId);
    }

    @Override
    public Page<TestReport> getAllReports(Pageable pageable) {
        // 默认按生成时间倒序排列
        if (pageable.getSort().isEmpty()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "generationTime"));
        }
        return testReportRepository.findAll(pageable);
    }

    @Override
    public List<TestReport> searchReportsByPlanName(String planName) {
        return testReportRepository.findByPlanNameContainingIgnoreCase(planName);
    }

    @Override
    public Page<TestReport> searchReportsByPlanName(String planName, Pageable pageable) {
        // 默认按生成时间倒序排列
        if (pageable.getSort().isEmpty()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "generationTime"));
        }
        return testReportRepository.findByPlanNameContainingIgnoreCase(planName, pageable);
    }

    @Override
    public void deleteReport(Long id) {
        testReportRepository.deleteById(id);
    }

    // 辅助方法：将Map转换为JSON字符串
    private String mapToJson(Map<String, Object> map) {
        try {
            String json = objectMapper.writeValueAsString(map);
            logger.debug("生成的JSON数据: {}", json);
            return json;
        } catch (Exception e) {
            logger.error("将Map转换为JSON失败", e);
            throw new RuntimeException("Failed to convert map to JSON", e);
        }
    }

    // 辅助方法：添加耗时对比数据到脚本详情
    private void addTimeComparisonData(Map<String, Object> detail, TestPlanExecutionLog log, 
                                       Map<Long, TestPlanExecutionLog> previousLogsMap) {
        Long scriptId = log.getTestScript().getId();
        if (previousLogsMap.containsKey(scriptId)) {
            TestPlanExecutionLog previousLog = previousLogsMap.get(scriptId);
            long currentTime = log.getExecutionTime();
            long previousTime = previousLog.getExecutionTime();
            long timeDiff = currentTime - previousTime;
            double timeDiffPercent = previousTime > 0 ? 
                    ((double) timeDiff / previousTime) * 100 : 0;
            
            detail.put("previousExecutionTime", previousTime);
            detail.put("timeDiff", timeDiff);
            detail.put("timeDiffPercent", timeDiffPercent);
            detail.put("timeDiffText", timeDiff >= 0 ? "+" + timeDiff : String.valueOf(timeDiff));
            
            String scriptName = log.getTestScript().getName();
            logger.debug("脚本 " + scriptName + " 耗时对比: 当前 " + currentTime + "ms, 上次 " + previousTime + "ms, 差异 " + timeDiff + "ms (" + String.format("%.2f", timeDiffPercent) + "%)");
        }
    }

    // 旧方法实现，保持兼容
    @Override
    public List<TestReport> getAllTestReports() {
        return testReportRepository.findAll();
    }

    @Override
    public TestReport getTestReportById(Long id) {
        return testReportRepository.findById(id).orElse(null);
    }

    @Override
    public TestReport createTestReport(TestReport testReport) {
        return testReportRepository.save(testReport);
    }

    @Override
    public TestReport updateTestReport(Long id, TestReport testReport) {
        if (testReportRepository.existsById(id)) {
            testReport.setId(id);
            return testReportRepository.save(testReport);
        }
        return null;
    }

    @Override
    public void deleteTestReport(Long id) {
        testReportRepository.deleteById(id);
    }
}