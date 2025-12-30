package com.testexecutionplatform.controller;

import com.testexecutionplatform.model.TestResult;
import com.testexecutionplatform.service.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-results")
public class TestResultController {

    @Autowired
    private TestResultService testResultService;

    @GetMapping
    public ResponseEntity<List<TestResult>> getAllTestResults() {
        List<TestResult> results = testResultService.getAllTestResults();
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestResult> getTestResultById(@PathVariable Long id) {
        TestResult result = testResultService.getTestResultById(id);
        if (result != null) {
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<TestResult> createTestResult(@RequestBody TestResult testResult) {
        TestResult createdResult = testResultService.createTestResult(testResult);
        return new ResponseEntity<>(createdResult, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestResult> updateTestResult(@PathVariable Long id, @RequestBody TestResult testResult) {
        TestResult updatedResult = testResultService.updateTestResult(id, testResult);
        if (updatedResult != null) {
            return new ResponseEntity<>(updatedResult, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestResult(@PathVariable Long id) {
        testResultService.deleteTestResult(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TestResult>> getTestResultsByTaskId(@PathVariable Long taskId) {
        List<TestResult> results = testResultService.getTestResultsByTaskId(taskId);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}