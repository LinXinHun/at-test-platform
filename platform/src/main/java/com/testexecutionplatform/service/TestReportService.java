package com.testexecutionplatform.service;

import com.testexecutionplatform.model.TestReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TestReportService {
    // 生成测试报告
    TestReport generateReport(Long executionId);
    
    // 根据ID获取测试报告
    Optional<TestReport> getReportById(Long id);
    
    // 根据执行记录ID获取测试报告
    Optional<TestReport> getReportByExecutionId(Long executionId);
    
    // 分页获取所有测试报告
    Page<TestReport> getAllReports(Pageable pageable);
    
    // 根据计划名称搜索测试报告
    List<TestReport> searchReportsByPlanName(String planName);
    
    // 根据计划名称分页搜索测试报告
    Page<TestReport> searchReportsByPlanName(String planName, Pageable pageable);
    
    // 删除测试报告
    void deleteReport(Long id);
    
    // 旧方法，保持兼容
    List<TestReport> getAllTestReports();
    TestReport getTestReportById(Long id);
    TestReport createTestReport(TestReport testReport);
    TestReport updateTestReport(Long id, TestReport testReport);
    void deleteTestReport(Long id);
}