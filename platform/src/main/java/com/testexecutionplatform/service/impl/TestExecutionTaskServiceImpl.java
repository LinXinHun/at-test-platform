package com.testexecutionplatform.service.impl;

import com.testexecutionplatform.model.ExecutionNode;
import com.testexecutionplatform.model.TestExecutionTask;
import com.testexecutionplatform.repository.TestExecutionTaskRepository;
import com.testexecutionplatform.service.ExecutionNodeService;
import com.testexecutionplatform.service.TestExecutionTaskService;
import com.testexecutionplatform.service.TestPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TestExecutionTaskServiceImpl implements TestExecutionTaskService {

    @Autowired
    private TestExecutionTaskRepository testExecutionTaskRepository;

    @Autowired
    private TestPlanService testPlanService;

    @Autowired
    private ExecutionNodeService executionNodeService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${execution.node.api.path:/api/execution/execute-task}")
    private String executionNodeApiPath;

    @Override
    public List<TestExecutionTask> getAllTestExecutionTasks() {
        return testExecutionTaskRepository.findAll();
    }

    @Override
    public TestExecutionTask getTestExecutionTaskById(Long id) {
        Optional<TestExecutionTask> optionalTestExecutionTask = testExecutionTaskRepository.findById(id);
        return optionalTestExecutionTask.orElse(null);
    }

    @Override
    public TestExecutionTask createTestExecutionTask(TestExecutionTask testExecutionTask) {
        // 设置创建时间
        testExecutionTask.setCreatedAt(LocalDateTime.now());
        testExecutionTask.setUpdatedAt(LocalDateTime.now());
        // 默认状态为待执行
        testExecutionTask.setStatus("PENDING");
        return testExecutionTaskRepository.save(testExecutionTask);
    }

    @Override
    public TestExecutionTask updateTestExecutionTask(Long id, TestExecutionTask testExecutionTask) {
        if (testExecutionTaskRepository.existsById(id)) {
            testExecutionTask.setId(id);
            testExecutionTask.setUpdatedAt(LocalDateTime.now());
            return testExecutionTaskRepository.save(testExecutionTask);
        }
        return null;
    }

    @Override
    public void deleteTestExecutionTask(Long id) {
        testExecutionTaskRepository.deleteById(id);
    }

    @Override
    public TestExecutionTask startTask(Long id) {
        TestExecutionTask task = getTestExecutionTaskById(id);
        if (task != null && "PENDING".equals(task.getStatus())) {
            // 选择可用的执行节点
            ExecutionNode availableNode = executionNodeService.getAvailableNode();
            
            if (availableNode != null) {
                // 构建节点URL
                String nodeUrl = "http://" + availableNode.getHost() + ":" + availableNode.getPort();
                
                // 更新任务状态为执行中
                task.setStatus("RUNNING");
                task.setExecutionNodeId(availableNode.getNodeId());
                task.setStartTime(LocalDateTime.now());
                task.setUpdatedAt(LocalDateTime.now());
                task = testExecutionTaskRepository.save(task);

                try {
                    // 调用执行节点API，直接传递任务ID
                    String apiUrl = nodeUrl + executionNodeApiPath + "?taskId=" + task.getId();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<Void> response = restTemplate.postForEntity(apiUrl, entity, Void.class);
                    
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        // 如果调用失败，更新任务状态为失败
                        task.setStatus("FAILED");
                        task.setErrorMessage("Failed to submit task to execution node");
                        task.setUpdatedAt(LocalDateTime.now());
                        task = testExecutionTaskRepository.save(task);
                    }
                } catch (Exception e) {
                    // 处理异常情况
                    task.setStatus("FAILED");
                    task.setErrorMessage("Error submitting task: " + e.getMessage());
                    task.setUpdatedAt(LocalDateTime.now());
                    task = testExecutionTaskRepository.save(task);
                }
            } else {
                // 没有可用节点，更新任务状态为失败
                task.setStatus("FAILED");
                task.setErrorMessage("No available execution nodes");
                task.setUpdatedAt(LocalDateTime.now());
                task = testExecutionTaskRepository.save(task);
            }
        }
        return task;
    }

    @Override
    public TestExecutionTask stopTask(Long id) {
        TestExecutionTask task = getTestExecutionTaskById(id);
        if (task != null && "RUNNING".equals(task.getStatus())) {
            // 这里可以添加向执行节点发送停止请求的逻辑
            task.setStatus("COMPLETED"); // 或者可以设置为STOPPED
            task.setEndTime(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            return testExecutionTaskRepository.save(task);
        }
        return null;
    }

    @Override
    public TestExecutionTask completeTask(Long id) {
        TestExecutionTask task = getTestExecutionTaskById(id);
        if (task != null && "RUNNING".equals(task.getStatus())) {
            // 更新任务状态为已完成
            task.setStatus("COMPLETED");
            task.setEndTime(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            return testExecutionTaskRepository.save(task);
        }
        return null;
    }

    @Override
    public List<TestExecutionTask> getTasksByPlanId(Long planId) {
        return testExecutionTaskRepository.findByPlanId(planId);
    }
}