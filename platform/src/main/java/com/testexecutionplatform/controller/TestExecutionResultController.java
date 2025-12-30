package com.testexecutionplatform.controller;

import com.testexecutionplatform.model.TestExecutionResult;
import com.testexecutionplatform.service.TestExecutionResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/execution-results")
public class TestExecutionResultController {
    
    @Autowired
    private TestExecutionResultService testExecutionResultService;

    @Value("${project.root.dir}")
    private String projectRootDir;
    
    // 根据脚本ID获取执行记录
    @GetMapping("/script/{scriptId}")
    public List<TestExecutionResult> getExecutionResultsByScriptId(@PathVariable Long scriptId) {
        return testExecutionResultService.getByScriptId(scriptId);
    }
    
    // 根据脚本ID获取最后一次执行记录
    @GetMapping("/script/{scriptId}/last")
    public TestExecutionResult getLastExecutionResultByScriptId(@PathVariable Long scriptId) {
        return testExecutionResultService.getLastByScriptId(scriptId);
    }
    
    // 获取单个执行记录详情
    @GetMapping("/{id}")
    public TestExecutionResult getExecutionResultById(@PathVariable Long id) {
        return testExecutionResultService.getById(id);
    }
    
    // 删除执行记录
    @DeleteMapping("/{id}")
    public void deleteExecutionResult(@PathVariable Long id) {
        testExecutionResultService.delete(id);
    }
    
    // 下载日志文件
    @GetMapping("/{id}/log")
    public ResponseEntity<Resource> downloadLogFile(@PathVariable Long id) throws IOException {
        // 获取执行结果
        TestExecutionResult executionResult = testExecutionResultService.getById(id);
        if (executionResult == null || executionResult.getLogFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 获取完整的日志文件路径
        String fullLogPath = projectRootDir + File.separator + executionResult.getLogFilePath();
        File logFile = new File(fullLogPath);
        
        if (!logFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        // 创建资源对象
        Resource resource = new FileSystemResource(logFile);
        
        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + logFile.getName());
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}