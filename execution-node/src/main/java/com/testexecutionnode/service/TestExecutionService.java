package com.testexecutionnode.service;

import com.testexecutionnode.config.NodeConfig;
import com.testexecutionnode.scheduler.TempFileCleanupScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class TestExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private NodeConfig nodeConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TempFileCleanupScheduler tempFileCleanupScheduler;

    @Value("${project.root.dir}")
    private String projectRootDir;

    // 根据操作系统类型获取命令行解释器和参数
    private List<String> getCommandInterpreter() {
        String osName = System.getProperty("os.name").toLowerCase();
        List<String> interpreter = new ArrayList<>();
        
        if (osName.contains("win")) {
            // Windows系统使用cmd.exe
            interpreter.add("cmd.exe");
            interpreter.add("/c");
        } else {
            // Linux/Mac系统使用bash
            interpreter.add("bash");
            interpreter.add("-c");
        }
        
        logger.info("Using command interpreter for OS {}: {}", osName, interpreter);
        return interpreter;
    }
    
    // 构建执行命令
    private String buildCommand(String scriptType, String filePath, String executionEndpointType) {
        String command = "";
        switch (scriptType.toLowerCase()) {
            case "py":
            case "python":
                // 根据操作系统类型选择Python命令
                String pythonCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
                if ("MiniApp".equals(executionEndpointType)) {
                    // 对于MiniApp类型，使用pytest命令格式
                    command = pythonCmd + " -m pytest " + filePath + " -vs";
                } else {
                    // 其他类型使用普通python命令
                    command = pythonCmd + " " + filePath;
                }
                break;
            case "sh":
                command = "bash " + filePath;
                break;
            case "js":
                command = "node " + filePath;
                break;
            case "java":
                // 编译并运行Java文件
                String className = filePath.substring(filePath.lastIndexOf('/') + 1, filePath.lastIndexOf('.'));
                String classPath = filePath.substring(0, filePath.lastIndexOf('/'));
                command = "javac -cp " + classPath + " " + filePath + " && java -cp " + classPath + " " + className;
                break;
            default:
                throw new IllegalArgumentException("Unsupported script type: " + scriptType);
        }
        logger.info("Built command for {} type with endpoint {}: {}", scriptType, executionEndpointType, command);
        return command;
    }

    // 下载文件并保存到临时目录
    private Path downloadScriptFile(Long planId, Long scriptId, String scriptName, String scriptType, String fileUrl) throws IOException {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));        
        
        Path projectRoot = Paths.get(projectRootDir);
        
        Path tempDir = projectRoot.resolve(nodeConfig.getTempScriptDirectory()).resolve(dateStr);
        if (planId != null) {
            tempDir = tempDir.resolve(String.valueOf(planId));
        }
        Files.createDirectories(tempDir);        
        
        String fileName = "test_" + scriptId + ".py";
        Path tempFilePath = tempDir.resolve(fileName);        
        
        String platformUrl = nodeConfig.getPlatformServerUrl();
        String downloadUrl = platformUrl + (platformUrl.endsWith("/") ? "" : "/") + "api/scripts/download?filePath=" + fileUrl;
        
        URL url = new URL(downloadUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, tempFilePath, StandardCopyOption.REPLACE_EXISTING);        
        }
        
        logger.info("Downloaded script to: {}", tempFilePath.toAbsolutePath().toString());        
        return tempFilePath;
    }
    
    // 将脚本类型转换为文件扩展名
    private String convertToFileExtension(String scriptType) {
        switch (scriptType.toLowerCase()) {
            case "python":
                return "py";
            case "java":
                return "java";
            case "shell":
                return "sh";
            case "batch":
                return "bat";
            default:
                return scriptType; // 如果是未知类型，直接使用scriptType作为扩展名
        }
    }

    // 推送测试结果到平台
    private void pushTestResult(Long taskId, Long scriptId, Map<String, Object> result) {
        try {
            String url;
            if (taskId != null) {
                // 单个测试任务
                url = nodeConfig.getPlatformServerUrl() + "/api/test-tasks/" + taskId + "/result";
            } else if (scriptId != null) {
                // 测试计划中的脚本
                url = nodeConfig.getPlatformServerUrl() + "/api/test-tasks/" + scriptId + "/plan-result";
            } else {
                logger.error("Both taskId and scriptId are null, cannot push test result");
                return;
            }

            logger.info("Pushing test result to: {}", url);

            // 构建请求体
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(result);
            logger.debug("Result JSON: {}", jsonBody);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Successfully pushed test result");
            } else {
                logger.error("Failed to push test result: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error pushing test result: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    // 更新测试计划状态
    private void updateExecutionPlanStatus(Long executionId, String status, int totalScripts, int successCount, int failedCount) {
        try {
            String url = nodeConfig.getPlatformServerUrl() + "/api/plan-executions/" + executionId;
            logger.info("Updating test plan execution status: {} (Execution ID: {})\n", status, executionId);
            
            // 构建请求体
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("status", status);
            statusData.put("totalScripts", totalScripts);
            statusData.put("successScripts", successCount);
            statusData.put("failedScripts", failedCount);
            
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(statusData);
            logger.debug("Status update JSON: {}", jsonBody);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Successfully updated test plan execution status");
            } else {
                logger.error("Failed to update test plan execution status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error updating test plan execution status: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    // 创建执行日志
    private Long createExecutionLog(Long scriptId, Long executionId) {        
        try {
            String url = nodeConfig.getPlatformServerUrl() + "/api/plan-executions/logs";
            logger.info("Creating execution log for scriptId: {}, executionId: {}", scriptId, executionId);

            // 构建执行日志对象
            Map<String, Object> logData = new HashMap<>();
            
            // 构建execution对象（只需要id字段）
            Map<String, Object> execution = new HashMap<>();
            execution.put("id", executionId);
            logData.put("execution", execution);
            
            // 构建testScript对象（只需要id字段）
            Map<String, Object> testScript = new HashMap<>();
            testScript.put("id", scriptId);
            logData.put("testScript", testScript);
            
            // 设置初始状态
            logData.put("status", "EXECUTING");

            // 构建请求
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(logData);
            logger.debug("Create log JSON: {}", jsonBody);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Long logId = Long.parseLong(response.getBody().get("id").toString());
                logger.info("Successfully created execution log with ID: {}", logId);
                return logId;
            } else {
                logger.error("Failed to create execution log: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error creating execution log: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 更新执行日志
    private void updateExecutionLog(Long logId, Map<String, Object> result) {
        try {
            String url = nodeConfig.getPlatformServerUrl() + "/api/plan-executions/logs/" + logId + "/status";
            logger.info("Updating execution log with ID: {}", logId);

            // 构建请求体
            Map<String, Object> logData = new HashMap<>();
            logData.put("status", result.get("status"));
            logData.put("result", result.get("output"));
            logData.put("errorMessage", result.get("error"));
            logData.put("executionTime", result.get("duration"));

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(logData);
            logger.debug("Log update JSON: {}", jsonBody);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Successfully updated execution log");
            } else {
                logger.error("Failed to update execution log: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error updating execution log: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public void executeTestTask(Long taskId) {
        logger.info("Starting test task execution: {}", taskId);
        Path tempFilePath = null;

        try {
            // 从平台获取测试任务详情
            String taskUrl = nodeConfig.getPlatformServerUrl() + "/api/test-tasks/" + taskId;
            ResponseEntity<String> taskResponse = restTemplate.getForEntity(taskUrl, String.class);
            if (taskResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Failed to get test task: {}", taskResponse.getStatusCode());
                return;
            }

            // 解析测试任务详情
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> testTask = mapper.readValue(taskResponse.getBody(), new TypeReference<Map<String, Object>>() {});
            Map<String, Object> script = (Map<String, Object>) testTask.get("script");

            // 获取脚本信息
            String scriptName = (String) script.get("name");
            String scriptType = (String) script.get("type");
            String scriptContent = (String) script.get("content");
            Long scriptId = ((Number) script.get("id")).longValue();
            // 获取计划ID（如果有）
            Map<String, Object> testPlan = (Map<String, Object>) testTask.get("testPlan");
            Long planId = testPlan != null ? ((Number) testPlan.get("id")).longValue() : null;

            logger.info("Executing script: {} (ID: {}, Type: {}, {})", scriptName, scriptId, scriptType, planId);

            // 保存脚本到临时文件
            tempFilePath = saveScriptToTempFile(scriptContent, scriptType, scriptId, planId);

            // 执行脚本 - 任务执行暂时不支持MiniApp类型的特殊命令，使用默认命令
            String command = buildCommand(scriptType, tempFilePath.toAbsolutePath().toString(), null);
            logger.info("Executing command: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder();
            List<String> interpreter = getCommandInterpreter();
            List<String> fullCommand = new ArrayList<>(interpreter);
            fullCommand.add(command);
            processBuilder.command(fullCommand);
            logger.info("ProcessBuilder command: {}", processBuilder.command());
            Process process = processBuilder.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 读取错误
            StringBuilder error = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }

            // 记录执行开始时间
            long startTime = System.currentTimeMillis();
            
            // 等待进程完成
            int exitCode = process.waitFor();
            
            // 计算执行时间
            long duration = System.currentTimeMillis() - startTime;

            // 构建执行结果
            Map<String, Object> executionResult = new HashMap<>();
            executionResult.put("status", exitCode == 0 ? "SUCCESS" : "FAILURE");
            executionResult.put("output", output.toString());
            executionResult.put("error", error.toString());
            executionResult.put("duration", duration);

            logger.info("Script execution completed with exit code: {}", exitCode);
            logger.info("Execution output: {}", output.toString());
            if (exitCode != 0) {
                logger.error("Execution error: {}", error.toString());
            }

            // 推送测试结果到平台
            pushTestResult(taskId, scriptId, executionResult);

        } catch (Exception e) {
            logger.error("Error executing test task: {}", e.getMessage());
            e.printStackTrace();

            // 推送错误结果到平台
            Map<String, Object> executionResult = new HashMap<>();
            executionResult.put("status", "FAILURE");
            executionResult.put("output", "");
            executionResult.put("error", e.getMessage());
            executionResult.put("duration", 0);

            pushTestResult(taskId, null, executionResult);
        } finally {
            // 不再直接删除临时脚本，而是保留以便后续查看和调试
            // 临时文件将由TempFileCleanupScheduler定时清理
            if (tempFilePath != null) {
                logger.info("Temporary script file preserved: {}", tempFilePath.toAbsolutePath().toString());
            }
        }
    }

    public void executeTestPlan(Long planId, Long executionId) {
        logger.info("Starting test plan execution: {}, execution ID: {}", planId, executionId);
        try {
            // 从平台获取测试计划详情（包含脚本列表）
            String planUrl = nodeConfig.getPlatformServerUrl() + "/api/plans/" + planId;
            ResponseEntity<String> planResponse = restTemplate.getForEntity(planUrl, String.class);
            if (planResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Failed to get test plan: {}", planResponse.getStatusCode());
                // 更新状态为失败，但不设置totalScripts为0
                updateExecutionPlanStatus(executionId, "FAILURE", 0, 0, 0);
                return;
            }

            // 解析测试计划和脚本列表
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> testPlan = mapper.readValue(planResponse.getBody(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> scripts = (List<Map<String, Object>>) testPlan.get("scripts");
            logger.info("Found {} scripts in test plan", scripts.size());

            int totalScripts = scripts.size();
            int successCount = 0;
            int failedCount = 0;

            // 更新测试计划状态为RUNNING
            updateExecutionPlanStatus(executionId, "EXECUTING", totalScripts, 0, 0);

            // 执行每个测试脚本
            for (Map<String, Object> script : scripts) {
                long scriptId = ((Number) script.get("id")).longValue();
                String scriptName = (String) script.get("name");
                String scriptType = (String) script.get("scriptType");
                String scriptContent = (String) script.get("content");
                String filePath = (String) script.get("filePath");
                Path tempFilePath = null;
                Map<String, Object> executionResult = new HashMap<>();
                Long logId = null;

                try {
                    // 创建执行日志
                    logId = createExecutionLog(scriptId, executionId);
                    logger.info("Created execution log with ID: {}", logId);

                    // 检查是否有filePath
                    if (filePath != null && !filePath.isEmpty()) {
                        logger.info("Found file path for script {}: {}", scriptId, filePath);
                        // 使用filePath下载脚本
                        tempFilePath = downloadScriptFile(planId, scriptId, scriptName, scriptType, filePath);
                        logger.info("Downloaded script to: {}", tempFilePath.toAbsolutePath());
                    } else {
                        logger.info("No file path found for script {}, using script content", scriptId);
                        // 保存脚本到临时文件
                        tempFilePath = saveScriptToTempFile(scriptContent, scriptType, scriptId, planId);
                        logger.info("Saved script to temporary file: {}", tempFilePath.toAbsolutePath());
                    }

                    // 获取执行端点类型
            String executionEndpointType = (String) testPlan.get("executionEndpointType");
            logger.info("Test plan execution endpoint type: {}", executionEndpointType);
            
            // 执行脚本
            executionResult = executeScript(tempFilePath, scriptType, scriptId, executionId, executionEndpointType);
                    
                    // 更新执行日志
                    if (logId != null) {
                        updateExecutionLog(logId, executionResult);
                        // 将日志内容推送到平台
                        pushLogContentToPlatform(String.valueOf(planId), String.valueOf(executionId), String.valueOf(logId), String.valueOf(scriptId), executionResult);
                    }

                    // 更新测试计划状态
                    if ("SUCCESS".equals(executionResult.get("status"))) {
                        successCount++;
                    } else {
                        failedCount++;
                    }
                    updateExecutionPlanStatus(executionId, "EXECUTING", totalScripts, successCount, failedCount);
                } catch (Exception e) {
                    logger.error("Error executing script {}: {}", scriptId, e.getMessage());
                    e.printStackTrace();
                    failedCount++;
                    updateExecutionPlanStatus(executionId, "EXECUTING", totalScripts, successCount, failedCount);
                    
                    // 构建错误结果
                    executionResult.put("status", "FAILURE");
                    executionResult.put("output", "");
                    executionResult.put("error", e.getMessage());
                    executionResult.put("duration", 0);
                    
                    // 更新执行日志
                    if (logId != null) {
                        updateExecutionLog(logId, executionResult);
                        // 将日志内容推送到平台
                        pushLogContentToPlatform(String.valueOf(planId), String.valueOf(executionId), String.valueOf(logId), String.valueOf(scriptId), executionResult);
                    }
                } finally {
                    // 不再直接删除临时脚本，而是保留以便后续查看和调试
                    // 临时文件将由TempFileCleanupScheduler定时清理
                    if (tempFilePath != null) {
                        logger.info("Temporary script file preserved: {}", tempFilePath.toAbsolutePath().toString());
                    }
                }
            }

            // 所有脚本执行完成后，更新测试计划状态
            String finalStatus = failedCount == 0 ? "SUCCESS" : "FAILURE";
            updateExecutionPlanStatus(executionId, finalStatus, totalScripts, successCount, failedCount);

            logger.info("Test plan execution completed. Total: {}, Success: {}, Failed: {}", totalScripts, successCount, failedCount);
        } catch (Exception e) {
            logger.error("Error executing test plan: {}", e.getMessage());
            e.printStackTrace();
            // 更新测试计划状态为失败
            updateExecutionPlanStatus(executionId, "FAILURE", 0, 0, 0);
        }
    }

    /**
     * 保存脚本到临时文件
     * @param scriptContent 脚本内容
     * @param scriptType 脚本类型
     * @param scriptId 脚本id
     * @param planId 计划id
     * @return 临时文件路径
     * @throws IOException IO异常
     */
    public Path saveScriptToTempFile(String scriptContent, String scriptType, Long scriptId, Long planId) throws IOException {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);        
        
        Path projectRoot = Paths.get(projectRootDir);
        
        Path tempDir = projectRoot.resolve(nodeConfig.getTempScriptDirectory()).resolve(dateStr);
        if (planId != null) {
            tempDir = tempDir.resolve(String.valueOf(planId));
        }
        Files.createDirectories(tempDir);        
        
        String fileName = "test_" + scriptId + ".py";
        Path tempFilePath = tempDir.resolve(fileName);        
        
        Files.write(tempFilePath, scriptContent.getBytes());
        logger.info("Saved script to temporary file: {}", tempFilePath.toAbsolutePath().toString());
        return tempFilePath;
    }

    /**
     * 执行脚本并返回结果
     * @param scriptPath 脚本路径
     * @param scriptType 脚本类型
     * @param scriptId 脚本ID
     * @param executionId 执行ID
     * @return 执行结果
     */
    public Map<String, Object> executeScript(Path scriptPath, String scriptType, Long scriptId, Long executionId, String executionEndpointType) {
        logger.info("Executing script: {} (Type: {})", scriptPath.getFileName(), scriptType);
        Map<String, Object> executionResult = new HashMap<>();
        long startTime = System.currentTimeMillis();
        // 移除重复的日志创建，由调用者负责创建日志
        // Long logId = createExecutionLog(scriptId, executionId);

        try {
            // 构建命令
            String command = buildCommand(scriptType, scriptPath.toAbsolutePath().toString(), executionEndpointType);
            logger.info("Executing command: {}", command);

            // 设置工作目录为scripts目录
            Path projectRoot = Paths.get(projectRootDir);
            Path scriptsDir = projectRoot.resolve("scripts");
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder();
            List<String> interpreter = getCommandInterpreter();
            List<String> fullCommand = new ArrayList<>(interpreter);
            fullCommand.add(command);
            processBuilder.command(fullCommand);
            processBuilder.directory(scriptsDir.toFile());
            logger.info("ProcessBuilder command: {} with directory: {}", processBuilder.command(), scriptsDir.toAbsolutePath());
            Process process = processBuilder.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 读取错误
            StringBuilder error = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 构建执行结果
            executionResult.put("status", exitCode == 0 ? "SUCCESS" : "FAILED");
            executionResult.put("output", output.toString());
            executionResult.put("error", error.toString());
            executionResult.put("duration", duration);

            logger.info("Script execution completed with exit code: {}", exitCode);
            logger.info("Execution output: {}", output.toString());
            if (exitCode != 0) {
                logger.error("Execution error: {}", error.toString());
            }

            // 日志更新由调用者负责，移除此处的更新逻辑
            // if (logId != null) {
            //     updateExecutionLog(logId, executionResult);
            // }

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            executionResult.put("status", "FAILED");
            executionResult.put("output", "");
            executionResult.put("error", "Execution exception: " + e.getMessage());
            executionResult.put("duration", duration);

            logger.error("Exception during script execution: {}", e.getMessage());
            e.printStackTrace();

            // 日志更新由调用者负责，移除此处的更新逻辑
            // if (logId != null) {
            //     updateExecutionLog(logId, executionResult);
            // }
        } finally {
            // 不再直接删除临时脚本，而是保留以便后续查看和调试
            // 临时文件将由TempFileCleanupScheduler定时清理
            if (scriptPath != null) {
                logger.info("Temporary script file preserved: {}", scriptPath.toAbsolutePath().toString());
            }
        }

        return executionResult;
    }

    /**
     * 更新执行日志并推送日志内容到平台
     */
    public void updateExecutionLog(String planId, String executionId, String logId, String scriptId, Map<String, Object> executionResult) {
        try {
            // 将日志内容推送到平台
            pushLogContentToPlatform(planId, executionId, logId, scriptId, executionResult);
        } catch (Exception e) {
            System.err.println("Error in new updateExecutionLog: " + e.getMessage());
            e.printStackTrace();
            // 可以选择抛出异常，由调用者处理
            throw new RuntimeException("Failed to update execution log", e);
        }
    }

    /**
     * 将日志内容推送到平台
     */
    private void pushLogContentToPlatform(String planId, String executionId, String logId, String scriptId, Map<String, Object> executionResult) throws JsonProcessingException {
        // 使用正确的API端点路径
        String url = nodeConfig.getPlatformServerUrl() + "/api/plan-executions/logs/upload";
        logger.info("Pushing log content to platform: {}", url);

        // 构建表单参数
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("planId", planId);
        formData.add("scriptId", scriptId); // 使用正确的scriptId参数
        formData.add("executionId", executionId);
        formData.add("logId", logId);
        
        // 获取日志内容（输出+错误）
        String output = executionResult.get("output") != null ? (String) executionResult.get("output") : "";
        String error = executionResult.get("error") != null ? (String) executionResult.get("error") : "";
        String logContent = output + "\n\n" + error;
        formData.add("logContent", logContent);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            logger.info("Successfully pushed log content to platform");
        } else {
            logger.error("Failed to push log content to platform: {}", response.getStatusCode());
            throw new RuntimeException("Failed to push log content to platform, status: " + response.getStatusCode());
        }
    }
}