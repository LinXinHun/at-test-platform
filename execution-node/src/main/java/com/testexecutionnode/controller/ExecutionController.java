package com.testexecutionnode.controller;

import com.testexecutionnode.service.TestExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/execution")
public class ExecutionController {

    @Autowired
    private TestExecutionService testExecutionService;

    @PostMapping("/execute-task")
    public ResponseEntity<String> executeTask(@RequestParam Long taskId) {
        try {
            // 异步执行测试任务
            Thread thread = new Thread(() -> {
                testExecutionService.executeTestTask(taskId);
            });
            thread.start();

            return ResponseEntity.ok("Test task execution started successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start test task execution: " + e.getMessage());
        }
    }
    
    @PostMapping("/execute-plan")
    public ResponseEntity<String> executePlan(@RequestBody Map<String, Object> params) {
        try {
            Long planId = ((Number) params.get("planId")).longValue();
            Long executionId = ((Number) params.get("executionId")).longValue();
            
            // 异步执行测试计划
            Thread thread = new Thread(() -> {
                testExecutionService.executeTestPlan(planId, executionId);
            });
            thread.start();

            return ResponseEntity.ok("Test plan execution started successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start test plan execution: " + e.getMessage());
        }
    }
    
    @PostMapping("/test-script-download")
    public ResponseEntity<String> testScriptDownload(@RequestBody Map<String, Object> params) {
        try {
            String scriptContent = (String) params.get("scriptContent");
            String scriptType = (String) params.get("scriptType");
            Long scriptId = ((Number) params.getOrDefault("scriptId", 1)).longValue();
            Long planId = ((Number) params.getOrDefault("planId", null)).longValue();
            
            // 保存脚本到临时文件
            java.nio.file.Path tempFilePath = testExecutionService.saveScriptToTempFile(scriptContent, scriptType, scriptId, planId);
            
            return ResponseEntity.ok("Script saved successfully to: " + tempFilePath.toAbsolutePath().toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save script: " + e.getMessage());
        }
    }

}