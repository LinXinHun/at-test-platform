package com.testexecutionplatform.service;

import com.testexecutionplatform.model.TestPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TestPlanService {
    List<TestPlan> getAllTestPlans();
    Page<TestPlan> getTestPlans(Pageable pageable);
    TestPlan getTestPlanById(Long id);
    TestPlan createTestPlan(TestPlan testPlan);
    TestPlan updateTestPlan(Long id, TestPlan testPlan);
    void deleteTestPlan(Long id);
}