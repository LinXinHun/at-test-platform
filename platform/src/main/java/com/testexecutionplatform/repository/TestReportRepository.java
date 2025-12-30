package com.testexecutionplatform.repository;

import com.testexecutionplatform.model.TestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TestReportRepository extends JpaRepository<TestReport, Long> {
    // 根据执行记录ID获取报告
    Optional<TestReport> findByExecutionId(Long executionId);
    
    // 根据计划名称搜索报告，忽略大小写
    List<TestReport> findByPlanNameContainingIgnoreCase(String planName);
    
    // 根据计划名称搜索报告，支持分页
    Page<TestReport> findByPlanNameContainingIgnoreCase(String planName, Pageable pageable);
}