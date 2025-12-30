package com.testexecutionplatform.controller;

import com.testexecutionplatform.model.TestPlanExecution;
import com.testexecutionplatform.model.TestPlanExecutionLog;
import com.testexecutionplatform.service.TestPlanExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/plan-executions")
public class TestPlanExecutionController {
    
    private static final Logger log = LoggerFactory.getLogger(TestPlanExecutionController.class);
    
    @Autowired
    private TestPlanExecutionService testPlanExecutionService;
    
    /**
     * 执行测试计划
     */
    @PostMapping
    public ResponseEntity<?> executeTestPlan(@RequestParam Long planId, @RequestParam(required = false) List<Long> nodeIdList) {
        try {
            TestPlanExecution execution = testPlanExecutionService.executeTestPlan(planId, nodeIdList);
            if (execution == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(execution);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid parameter: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to execute test plan: " + e.getMessage());
        }
    }
    
    /**
     * 根据执行ID获取执行记录
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<?> getExecutionById(@PathVariable Long executionId) {
        try {
            TestPlanExecution execution = testPlanExecutionService.getExecutionById(executionId);
            if (execution == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(execution);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get execution: " + e.getMessage());
        }
    }
    
    /**
     * 根据测试计划ID获取执行记录列表
     */
    @GetMapping("/plan/{planId}")
    public ResponseEntity<?> getExecutionsByPlanId(@PathVariable Long planId) {
        try {
            List<TestPlanExecution> executions = testPlanExecutionService.getExecutionsByPlanId(planId);
            return ResponseEntity.ok(executions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get executions: " + e.getMessage());
        }
    }
    
    /**
     * 根据执行ID获取执行日志（不分页）
     */
    @GetMapping("/{executionId}/logs")
    public List<TestPlanExecutionLog> getExecutionLogsByExecutionId(@PathVariable Long executionId) {
        return testPlanExecutionService.getExecutionLogsByExecutionId(executionId);
    }
    
    /**
     * 根据执行ID分页获取执行日志
     */
    @GetMapping("/{executionId}/logs/page")
    public Page<TestPlanExecutionLog> getExecutionLogsByExecutionId(@PathVariable Long executionId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return testPlanExecutionService.getExecutionLogsByExecutionId(executionId, pageable);
    }
    
    /**
     * 创建执行日志
     */
    @PostMapping("/logs")
    public TestPlanExecutionLog createExecutionLog(@RequestBody TestPlanExecutionLog log) {
        return testPlanExecutionService.createExecutionLog(log);
    }
    
    /**
     * 更新执行计划状态
     */
    @PutMapping("/{executionId}")
    public ResponseEntity<?> updateExecutionStatus(@PathVariable Long executionId, @RequestBody Map<String, Object> updateData) {
        try {
            String status = (String) updateData.get("status");
            Integer totalScripts = (Integer) updateData.get("totalScripts");
            Integer successScripts = (Integer) updateData.get("successScripts");
            Integer failedScripts = (Integer) updateData.get("failedScripts");
            
            TestPlanExecution execution = testPlanExecutionService.getExecutionById(executionId);
            if (execution != null) {
                execution.setStatus(status);
                if (totalScripts != null) execution.setTotalScripts(totalScripts);
                if (successScripts != null) execution.setSuccessScripts(successScripts);
                if (failedScripts != null) execution.setFailedScripts(failedScripts);
                if ("SUCCESS".equals(status) || "FAILURE".equals(status)) {
                    execution.setEndTime(LocalDateTime.now());
                }
                TestPlanExecution updatedExecution = testPlanExecutionService.updateExecution(execution);
                return ResponseEntity.ok(updatedExecution);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update execution: " + e.getMessage());
        }
    }
    
    /**
     * 更新执行日志状态
     */
    @PutMapping("/logs/{logId}/status")
    public ResponseEntity<TestPlanExecutionLog> updateExecutionLogStatus(
            @PathVariable Long logId, 
            @RequestBody Map<String, Object> updateData) {
        
        String status = (String) updateData.get("status");
        String result = (String) updateData.get("result");
        String errorMessage = (String) updateData.get("errorMessage");
        Long executionTime = updateData.get("executionTime") != null ? ((Number) updateData.get("executionTime")).longValue() : null;
        
        testPlanExecutionService.updateExecutionLogStatus(logId, status, result, errorMessage, executionTime);
        
        // 返回更新后的日志
        TestPlanExecutionLog updatedLog = testPlanExecutionService.getExecutionLogById(logId);
        
        return updatedLog != null ? ResponseEntity.ok(updatedLog) : ResponseEntity.notFound().build();
    }
    
    // 异常处理
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParams(MissingServletRequestParameterException ex) {
        String paramName = ex.getParameterName();
        return ResponseEntity.badRequest().body("Missing parameter: " + paramName);
    }
    
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<?> handleNumberFormatException(NumberFormatException ex) {
        return ResponseEntity.badRequest().body("Invalid number format: " + ex.getMessage());
    }
    
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 接收执行节点推送的日志内容
     */
    @PostMapping(value = "/logs/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadLogContent(@RequestParam("planId") String planId, 
                                                 @RequestParam("scriptId") String scriptId,
                                                 @RequestParam("executionId") String executionId,
                                                 @RequestParam("logId") String logId,
                                                 @RequestParam("logContent") String logContent) {
        try {
            // 确保日志目录存在
            String logDir = "logs/" + planId + "/" + executionId;
            Path logDirPath = Paths.get(logDir);
            Files.createDirectories(logDirPath);
            
            // 保存日志内容到文件
            String logFilePath = logDir + "/" + scriptId + ".log";
            Files.write(Paths.get(logFilePath), logContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            log.info("Log content saved successfully for plan {}, script {}, execution {}", planId, scriptId, executionId);
            return ResponseEntity.ok("Log content uploaded successfully");
        } catch (Exception e) {
            log.error("Error saving log content: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload log content");
        }
    }
    
    /**
     * 下载执行日志文件
     */
    @GetMapping("/logs/download")
    public ResponseEntity<Resource> downloadLogFile(@RequestParam("planId") String planId, 
                                                 @RequestParam("executionId") String executionId,
                                                 @RequestParam("scriptId") String scriptId) {
        try {
            // 构建日志文件路径
            String logFilePath = "logs/" + planId + "/" + executionId + "/" + scriptId + ".log";
            Path path = Paths.get(logFilePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + scriptId + ".log")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error downloading log file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}