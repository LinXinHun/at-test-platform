package com.testexecutionplatform.service.impl;

import com.testexecutionplatform.model.TestExecutionResult;
import com.testexecutionplatform.model.TestScript;
import com.testexecutionplatform.repository.TestScriptRepository;
import com.testexecutionplatform.repository.TestExecutionResultRepository;
import com.testexecutionplatform.service.TestExecutionResultService;
import com.testexecutionplatform.service.TestScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.io.*;
import java.io.PrintWriter;

@Service
public class TestScriptServiceImpl implements TestScriptService {

    @Autowired
    private TestScriptRepository testScriptRepository;

    @Autowired
    private TestExecutionResultService testExecutionResultService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${project.root.dir}")
    private String projectRootDir;

    private static final Logger logger = Logger.getLogger(TestScriptServiceImpl.class.getName());
    private static final String SCRIPTS_DIR = "scripts/";

    @Override
    public List<TestScript> getAllTestScripts() {
        return testScriptRepository.findAll();
    }

    @Override
    public Page<TestScript> getTestScripts(Pageable pageable) {
        return testScriptRepository.findAll(pageable);
    }

    @Override
    public TestScript getTestScriptById(Long id) {
        Optional<TestScript> optionalTestScript = testScriptRepository.findById(id);
        return optionalTestScript.orElse(null);
    }

    @Override
    public TestScript createTestScript(TestScript testScript, MultipartFile file) {
        try {
            // 先保存脚本到数据库以获取ID
            TestScript savedScript = testScriptRepository.save(testScript);
            Long scriptId = savedScript.getId();
            
            // 获取项目根目录
            String scriptsPath = projectRootDir + File.separator + SCRIPTS_DIR;
            
            // 创建scripts目录（如果不存在）
            File scriptsDir = new File(scriptsPath);
            if (!scriptsDir.exists()) {
                scriptsDir.mkdirs();
            }
            
            // 使用脚本ID生成文件名
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String uniqueFileName = "script_" + scriptId + fileExtension;
            String fullFilePath = scriptsPath + uniqueFileName;
            
            // 保存文件
            File newFile = new File(fullFilePath);
            try (InputStream inputStream = file.getInputStream();
                 OutputStream outputStream = new FileOutputStream(newFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            // 设置脚本文件路径
            savedScript.setFilePath(uniqueFileName);
            
            // 读取文件内容并设置到scriptContent字段
            try (InputStream inputStream = file.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
                savedScript.setScriptContent(content.toString());
            }
            
            // 更新到数据库
            return testScriptRepository.save(savedScript);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create test script: " + e.getMessage());
        }
    }

    @Override
    public TestScript updateTestScript(Long id, TestScript testScript, MultipartFile file) {
        TestScript existingScript = getTestScriptById(id);
        if (existingScript != null) {
            try {
                // 获取项目根目录
                String scriptsPath = projectRootDir + File.separator + SCRIPTS_DIR;
                
                // 创建scripts目录（如果不存在）
                File scriptsDir = new File(scriptsPath);
                if (!scriptsDir.exists()) {
                    scriptsDir.mkdirs();
                }
                
                // 删除旧文件
                String oldFilePath = existingScript.getFilePath();
                if (oldFilePath != null) {
                    // 彻底处理路径：提取文件名，不管前面有什么路径
                    File fileObj = new File(oldFilePath);
                    String fileName = fileObj.getName();
                    
                    String fullOldFilePath = scriptsPath + fileName;
                    File oldFile = new File(fullOldFilePath);
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                }
                
                // 保存新文件
                // 使用脚本ID生成文件名
                String originalFileName = file.getOriginalFilename();
                String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String uniqueFileName = "script_" + id + fileExtension;
                String fullFilePath = scriptsPath + uniqueFileName;
                
                // 保存文件
                File newFile = new File(fullFilePath);
                try (InputStream inputStream = file.getInputStream();
                     OutputStream outputStream = new FileOutputStream(newFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                
                // 更新脚本信息
                testScript.setId(id);
                testScript.setFilePath(uniqueFileName);
                
                // 读取文件内容并设置到scriptContent字段
                try (InputStream inputStream = file.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append(System.lineSeparator());
                    }
                    testScript.setScriptContent(content.toString());
                }
                
                // 保存到数据库
                return testScriptRepository.save(testScript);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to update test script: " + e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void deleteTestScript(Long id) {
        TestScript testScript = getTestScriptById(id);
        if (testScript != null) {
            try {
                // 删除文件
                String filePath = testScript.getFilePath();
                if (filePath != null) {
                    // 获取项目根目录
                    String scriptsPath = projectRootDir + File.separator + SCRIPTS_DIR;
                    
                    // 彻底处理路径：提取文件名，不管前面有什么路径
                    File fileObj = new File(filePath);
                    String fileName = fileObj.getName();
                    
                    String fullFilePath = scriptsPath + fileName;
                    File file = new File(fullFilePath);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                
                // 删除数据库记录
                testScriptRepository.deleteById(id);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to delete test script: " + e.getMessage());
            }
        }
    }

    @Override
    public Map<String, Object> executeTestScript(Long id) {
        TestScript testScript = getTestScriptById(id);
        if (testScript == null) {
            throw new RuntimeException("Test script not found with id: " + id);
        }

        Map<String, Object> result = new HashMap<>();
        TestExecutionResult executionResult = new TestExecutionResult();
        executionResult.setTestScript(testScript); // 使用setTestScript方法，而不是setScriptId
        executionResult.setStartTime(LocalDateTime.now());

        try {
            // 获取脚本文件路径
            String filePath = testScript.getFilePath();
            if (filePath == null) {
                throw new RuntimeException("Script file path is null");
            }
            
            // 构建完整的文件路径
            String fullFilePath = projectRootDir + File.separator + SCRIPTS_DIR + filePath;
            File scriptFile = new File(fullFilePath);
            
            if (!scriptFile.exists()) {
                throw new RuntimeException("Script file not found: " + fullFilePath);
            }

            // 设置执行超时时间
            long timeout = testScript.getTimeout() > 0 ? testScript.getTimeout() : 30; // 默认30秒

            // 执行脚本
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (testScript.getScriptType().equalsIgnoreCase("python")) {
                processBuilder.command("python3", scriptFile.getAbsolutePath());
            } else {
                throw new RuntimeException("Unsupported script type: " + testScript.getScriptType());
            }

            // 捕获输出和错误
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            // 读取输出和错误
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            
            // 使用线程读取输出和错误
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append(System.lineSeparator());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            
            outputThread.start();
            errorThread.start();

            // 等待进程完成或超时
            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!completed) {
                process.destroy();
                String timeoutOutput = "Script execution timed out after " + timeout + " seconds";
                // 限制输出到30个字符
                String limitedOutput = timeoutOutput.length() > 30 ? timeoutOutput.substring(0, 30) + "..." : timeoutOutput;
                executionResult.setStatus("timeout");
                executionResult.setError(timeoutOutput); // 超时也视为错误，写入error字段
                result.put("status", "timeout");
                result.put("error", timeoutOutput);
                result.put("success", false);
            } else {
                // 等待线程完成
                outputThread.join();
                errorThread.join();
                
                int exitCode = process.exitValue();
                String fullOutput = output.toString().trim();
                String fullError = error.toString().trim();
                
                if (exitCode == 0) {
                    // 成功执行
                    // 限制输出到30个字符
                    String limitedOutput = fullOutput.length() > 30 ? fullOutput.substring(0, 30) + "..." : fullOutput;
                    executionResult.setStatus("success");
                    executionResult.setOutput(limitedOutput);
                    result.put("status", "success");
                    result.put("output", fullOutput);
                    result.put("success", true);
                } else {
                    // 执行失败
                    String errorMsg = !fullError.isEmpty() ? fullError : "Script execution failed with exit code: " + exitCode;
                    executionResult.setStatus("failure");
                    executionResult.setError(errorMsg); // 写入error字段，不限制长度
                    result.put("status", "failure");
                    result.put("error", errorMsg);
                    result.put("success", false);
                }
            }

            result.put("scriptId", id);

        } catch (Exception e) {
            String errorOutput = "Error executing script: " + e.getMessage();
            executionResult.setStatus("failure");
            executionResult.setError(errorOutput); // 写入error字段，不限制长度
            result.put("status", "failure");
            result.put("error", errorOutput);
            result.put("scriptId", id);
            e.printStackTrace();
        } finally {
            executionResult.setEndTime(LocalDateTime.now());
            
            // 计算执行时间（毫秒）
            if (executionResult.getStartTime() != null && executionResult.getEndTime() != null) {
                long executionTime = java.time.Duration.between(executionResult.getStartTime(), executionResult.getEndTime()).toMillis();
                executionResult.setExecutionTime(executionTime);
            }
            
            // 创建并设置日志文件路径
            try {
                String logsDir = projectRootDir + File.separator + "logs" + File.separator + "simple" + File.separator + String.valueOf(id);
                File logsDirectory = new File(logsDir);
                if (!logsDirectory.exists()) {
                    logsDirectory.mkdirs();
                }
                
                // 创建日志文件
                String logFileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".log";
                String logFilePath = logsDir + File.separator + logFileName;
                File logFile = new File(logFilePath);
                
                // 将执行输出写入日志文件
                try (PrintWriter writer = new PrintWriter(logFile, "UTF-8")) {
                    writer.println("Test Script Execution Log");
                    writer.println("========================");
                    writer.println("Script ID: " + id);
                    writer.println("Script Name: " + testScript.getName());
                    writer.println("Start Time: " + executionResult.getStartTime());
                    writer.println("End Time: " + executionResult.getEndTime());
                    writer.println("Execution Time: " + executionResult.getExecutionTime() + " ms");
                    writer.println("Status: " + executionResult.getStatus());
                    writer.println("Output:");
                    writer.println(executionResult.getOutput() != null ? executionResult.getOutput() : "N/A");
                    writer.println("Error:");
                    writer.println(executionResult.getError() != null ? executionResult.getError() : "N/A");
                }
                
                // 设置日志文件路径（相对于项目根目录）
                executionResult.setLogFilePath(logFilePath.substring(projectRootDir.length() + 1));
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // 保存执行结果到数据库
            testExecutionResultService.save(executionResult);
        }

        return result;
    }
}