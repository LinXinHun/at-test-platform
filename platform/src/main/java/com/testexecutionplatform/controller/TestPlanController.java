package com.testexecutionplatform.controller;

import com.testexecutionplatform.model.TestPlan;
import com.testexecutionplatform.service.TestPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/plans")
public class TestPlanController {
    private static final Logger logger = Logger.getLogger(TestPlanController.class.getName());

    @Autowired
    private TestPlanService testPlanService;

    @GetMapping
    public ResponseEntity<Page<TestPlan>> getAllTestPlans(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TestPlan> plans = testPlanService.getTestPlans(pageable);
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestPlan> getTestPlanById(@PathVariable Long id) {
        TestPlan plan = testPlanService.getTestPlanById(id);
        if (plan != null) {
            return new ResponseEntity<>(plan, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<TestPlan> createTestPlan(@RequestBody TestPlan testPlan) {
        logger.info("Creating test plan with name: " + testPlan.getName());
        logger.info("Test plan description: " + testPlan.getDescription());
        logger.info("Test plan scriptIds: " + testPlan.getScriptIds());
        
        TestPlan createdPlan = testPlanService.createTestPlan(testPlan);
        return new ResponseEntity<>(createdPlan, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestPlan> updateTestPlan(@PathVariable Long id, @RequestBody TestPlan testPlan) {
        TestPlan updatedPlan = testPlanService.updateTestPlan(id, testPlan);
        if (updatedPlan != null) {
            return new ResponseEntity<>(updatedPlan, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestPlan(@PathVariable Long id) {
        testPlanService.deleteTestPlan(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 全局异常处理
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex, WebRequest request) {
        logger.severe("Error creating test plan: " + ex.getMessage());
        ex.printStackTrace();
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}