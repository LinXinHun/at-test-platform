package com.testexecutionplatform.service.impl;

import com.testexecutionplatform.model.*;
import com.testexecutionplatform.repository.TestPlanExecutionLogRepository;
import com.testexecutionplatform.repository.TestPlanExecutionRepository;
import com.testexecutionplatform.repository.TestPlanRepository;
import com.testexecutionplatform.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TestPlanExecutionServiceImpl implements TestPlanExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TestPlanExecutionServiceImpl.class);

    @Autowired
    private TestPlanExecutionRepository testPlanExecutionRepository;

    @Autowired
    private TestPlanExecutionLogRepository testPlanExecutionLogRepository;

    @Autowired
    private TestPlanRepository testPlanRepository;

    @Autowired
    private TestPlanService testPlanService;

    @Autowired
    private ExecutionNodeService executionNodeService;

    @Autowired
    private TestScriptService testScriptService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${execution.node.api.path:/api/execution/execute-plan}")
    private String executionNodeApiPath;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 执行测试计划
     */
    @Override
    public TestPlanExecution executeTestPlan(Long planId, List<Long> nodeIdList) {
        if (planId == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }
        
        // 获取测试计划
        TestPlan testPlan = testPlanService.getTestPlanById(planId);
        if (testPlan == null) {
            throw new IllegalArgumentException("Test plan not found with ID: " + planId);
        }
        
        // 确定要使用的执行节点
        ExecutionNode executionNode;
        if (nodeIdList != null && !nodeIdList.isEmpty()) {
            // 使用指定的第一个节点
            Long nodeId = nodeIdList.get(0);
            Optional<ExecutionNode> optionalNode = executionNodeService.getExecutionNodeById(nodeId);
            if (!optionalNode.isPresent()) {
                throw new IllegalArgumentException("Execution node not found with ID: " + nodeId);
            }
            executionNode = optionalNode.get();
        } else {
            // 自动选择一个在线的执行节点
            List<ExecutionNode> onlineNodes = executionNodeService.getOnlineExecutionNodes();
            if (onlineNodes.isEmpty()) {
                throw new IllegalArgumentException("No online execution nodes available");
            }
            // 简单地选择第一个在线节点
            executionNode = onlineNodes.get(0);
        }
        
        // 检查节点状态
        if (!"ONLINE".equals(executionNode.getStatus())) {
            throw new IllegalArgumentException("Execution node is not online: " + executionNode.getId());
        }
        
        // 创建执行记录
        TestPlanExecution execution = new TestPlanExecution();
        execution.setTestPlan(testPlan);
        execution.setExecutionNode(executionNode); // 设置执行节点
        execution.setStatus("EXECUTING");
        // 修复：设置正确的脚本总数，而不是固定值0
        execution.setTotalScripts(testPlan.getScripts().size());
        execution.setSuccessScripts(0);
        execution.setFailedScripts(0);
        execution.setStartTime(LocalDateTime.now());
        
        // 保存执行记录
        execution = testPlanExecutionRepository.save(execution);
        
        // 创建最终变量副本用于lambda表达式
        final TestPlanExecution finalExecution = execution;
        final Long finalNodeId = executionNode.getId();
        
        // 异步调用执行节点API
        executorService.submit(() -> {
            try {
                String url = "http://" + executionNode.getHost() + ":" + executionNode.getPort() + executionNodeApiPath;
                
                // 构建请求参数
                Map<String, Object> params = new HashMap<>();
                params.put("executionId", finalExecution.getId());
                params.put("planId", planId);
                
                // 设置请求头为JSON格式
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, headers);
                
                // 调用执行节点API
                restTemplate.postForEntity(url, requestEntity, String.class);
            } catch (Exception e) {
                logger.error("Failed to execute test plan on node {}", finalNodeId, e);
            }
        });
        
        return execution;
    }

    @Override
    public TestPlanExecution getExecutionById(Long executionId) {
        return testPlanExecutionRepository.findById(executionId).orElse(null);
    }

    @Override
    public List<TestPlanExecution> getExecutionsByPlanId(Long planId) {
        return testPlanExecutionRepository.findByTestPlanIdOrderByCreatedAtDesc(planId);
    }

    @Override
    public Page<TestPlanExecutionLog> getExecutionLogsByExecutionId(Long executionId, Pageable pageable) {
        TestPlanExecution execution = testPlanExecutionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return Page.empty();
        }
        return testPlanExecutionLogRepository.findByExecutionIdOrderByCreatedAtAsc(executionId, pageable);
    }

    @Override
    public List<TestPlanExecutionLog> getExecutionLogsByExecutionId(Long executionId) {
        return testPlanExecutionLogRepository.findByExecutionIdOrderByCreatedAtAsc(executionId);
    }

    @Override
    public void updateExecutionStatus(Long executionId, String status) {
        TestPlanExecution execution = testPlanExecutionRepository.findById(executionId).orElse(null);
        if (execution != null) {
            execution.setStatus(status);
            if ("SUCCESS".equals(status) || "FAILURE".equals(status)) {
                execution.setEndTime(LocalDateTime.now());
            }
            final TestPlanExecution updatedExecution = testPlanExecutionRepository.save(execution);

            // 更新测试计划最后执行状态
            TestPlan testPlan = testPlanRepository.findById(execution.getTestPlan().getId()).orElse(null);
            if (testPlan != null) {
                testPlan.setLastExecutionStatus(status);
                if ("SUCCESS".equals(status) || "FAILURE".equals(status)) {
                    testPlan.setLastExecutionTime(updatedExecution.getEndTime());
                }
                testPlanRepository.save(testPlan);
            }
        }
    }

    @Override
    public void updateExecutionLogStatus(Long logId, String status, String result, String errorMessage, Long executionTime) {
        TestPlanExecutionLog log = testPlanExecutionLogRepository.findById(logId).orElse(null);
        if (log != null) {
            log.setStatus(status);
            // 将result字段只保留前30个字符
            if (result != null && !result.isEmpty()) {
                if (result.length() > 30) {
                    result = result.substring(0, 30) + "...";
                }
            }
            log.setResult(result);
            log.setErrorMessage(errorMessage);
            log.setExecutionTime(executionTime);
            log.setEndTime(LocalDateTime.now());
            testPlanExecutionLogRepository.save(log);

            // 更新执行记录的统计信息
            TestPlanExecution execution = testPlanExecutionRepository.findById(log.getExecution().getId()).orElse(null);
            if (execution != null) {
                if ("SUCCESS".equals(status)) {
                    execution.setSuccessScripts(execution.getSuccessScripts() + 1);
                } else if ("FAILURE".equals(status)) {
                    execution.setFailedScripts(execution.getFailedScripts() + 1);
                }

                // 检查是否所有脚本都已执行完成
                if (execution.getSuccessScripts() + execution.getFailedScripts() == execution.getTotalScripts()) {
                    execution.setStatus(execution.getFailedScripts() > 0 ? "FAILURE" : "SUCCESS");
                    execution.setEndTime(LocalDateTime.now());
                }

                testPlanExecutionRepository.save(execution);

                // 更新测试计划最后执行状态
                TestPlan testPlan = testPlanRepository.findById(execution.getTestPlan().getId()).orElse(null);
                if (testPlan != null) {
                    testPlan.setLastExecutionStatus(execution.getStatus());
                    if (execution.getEndTime() != null) {
                        testPlan.setLastExecutionTime(execution.getEndTime());
                    }
                    testPlanRepository.save(testPlan);
                }
            }
        }
    }
    
    @Override
    public TestPlanExecutionLog createExecutionLog(TestPlanExecutionLog log) {
        // 确保执行对象存在
        TestPlanExecution execution = testPlanExecutionRepository.findById(log.getExecution().getId())
                .orElseThrow(() -> new RuntimeException("Execution not found: " + log.getExecution().getId()));
        log.setExecution(execution);
        
        // 确保脚本对象存在
        TestScript testScript = testScriptService.getTestScriptById(log.getTestScript().getId());
        if (testScript == null) {
            throw new RuntimeException("Test script not found: " + log.getTestScript().getId());
        }
        log.setTestScript(testScript);
        
        // 设置默认值
        if (log.getStartTime() == null) {
            log.setStartTime(LocalDateTime.now());
        }
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(LocalDateTime.now());
        }
        if (log.getUpdatedAt() == null) {
            log.setUpdatedAt(LocalDateTime.now());
        }
        
        return testPlanExecutionLogRepository.save(log);
    }
    
    @Override
    public TestPlanExecution updateExecution(TestPlanExecution execution) {
        // 更新测试计划的最后执行状态
        TestPlan testPlan = testPlanRepository.findById(execution.getTestPlan().getId()).orElse(null);
        if (testPlan != null) {
            testPlan.setLastExecutionStatus(execution.getStatus());
            if (execution.getEndTime() != null) {
                testPlan.setLastExecutionTime(execution.getEndTime());
            }
            testPlanRepository.save(testPlan);
        }
        
        return testPlanExecutionRepository.save(execution);
    }
    
    @Override
    public TestPlanExecutionLog getExecutionLogById(Long logId) {
        return testPlanExecutionLogRepository.findById(logId).orElse(null);
    }

}