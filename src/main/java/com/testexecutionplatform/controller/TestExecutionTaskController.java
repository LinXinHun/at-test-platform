package com.testexecutionplatform.controller;

import com.testexecutionplatform.model.TestExecutionTask;
import com.testexecutionplatform.service.TestExecutionTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TestExecutionTaskController {

    @Autowired
    private TestExecutionTaskService testExecutionTaskService;

    @GetMapping
    public ResponseEntity<List<TestExecutionTask>> getAllTestExecutionTasks() {
        List<TestExecutionTask> tasks = testExecutionTaskService.getAllTestExecutionTasks();
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestExecutionTask> getTestExecutionTaskById(@PathVariable Long id) {
        TestExecutionTask task = testExecutionTaskService.getTestExecutionTaskById(id);
        if (task != null) {
            return new ResponseEntity<>(task, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<TestExecutionTask> createTestExecutionTask(@RequestBody TestExecutionTask testExecutionTask) {
        TestExecutionTask createdTask = testExecutionTaskService.createTestExecutionTask(testExecutionTask);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestExecutionTask> updateTestExecutionTask(@PathVariable Long id, @RequestBody TestExecutionTask testExecutionTask) {
        TestExecutionTask updatedTask = testExecutionTaskService.updateTestExecutionTask(id, testExecutionTask);
        if (updatedTask != null) {
            return new ResponseEntity<>(updatedTask, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestExecutionTask(@PathVariable Long id) {
        testExecutionTaskService.deleteTestExecutionTask(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<TestExecutionTask> startTask(@PathVariable Long id) {
        TestExecutionTask task = testExecutionTaskService.startTask(id);
        if (task != null) {
            return new ResponseEntity<>(task, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<TestExecutionTask> stopTask(@PathVariable Long id) {
        TestExecutionTask task = testExecutionTaskService.stopTask(id);
        if (task != null) {
            return new ResponseEntity<>(task, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
