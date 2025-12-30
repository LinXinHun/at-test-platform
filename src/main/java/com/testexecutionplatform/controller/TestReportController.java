package com.testexecutionplatform.controller;

import com.testexecutionplatform.model.TestReport;
import com.testexecutionplatform.service.TestReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class TestReportController {
    
    @Autowired
    private TestReportService testReportService;
    
    // 获取报告详情
    @GetMapping("/{reportId}")
    public ResponseEntity<TestReport> getReportById(@PathVariable Long reportId) {
        Optional<TestReport> report = testReportService.getReportById(reportId);
        return report.map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // 根据执行记录ID获取报告
    @GetMapping("/execution/{executionId}")
    public ResponseEntity<TestReport> getReportByExecutionId(@PathVariable Long executionId) {
        Optional<TestReport> report = testReportService.getReportByExecutionId(executionId);
        return report.map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // 分页获取所有报告（默认每页10条，倒序排列）
    @GetMapping
    public ResponseEntity<Page<TestReport>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TestReport> reports = testReportService.getAllReports(pageable);
        return ResponseEntity.ok(reports);
    }

    // 删除测试报告
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long reportId) {
        testReportService.deleteReport(reportId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    // 根据计划名称分页搜索报告
    @GetMapping("/search")
    public ResponseEntity<Page<TestReport>> searchReportsByPlanName(
            @RequestParam String planName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TestReport> reports = testReportService.searchReportsByPlanName(planName, pageable);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * 生成测试报告
     */
    @PostMapping("/generate/{executionId}")
    public ResponseEntity<?> generateReport(@PathVariable Long executionId) {
        try {
            TestReport report = testReportService.generateReport(executionId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid parameter: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate test report: " + e.getMessage());
        }
    }
}