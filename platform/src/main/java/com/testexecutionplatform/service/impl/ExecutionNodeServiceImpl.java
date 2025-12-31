package com.testexecutionplatform.service.impl;

import com.testexecutionplatform.model.ExecutionNode;
import com.testexecutionplatform.repository.ExecutionNodeRepository;
import com.testexecutionplatform.service.ExecutionNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExecutionNodeServiceImpl implements ExecutionNodeService {
    
    @Autowired
    private ExecutionNodeRepository executionNodeRepository;
    
    @Override
    public List<ExecutionNode> getAllExecutionNodes() {
        return executionNodeRepository.findAll();
    }
    
    @Override
    public Optional<ExecutionNode> getExecutionNodeById(Long id) {
        return executionNodeRepository.findById(id);
    }
    
    @Override
    public Optional<ExecutionNode> getExecutionNodeByNodeId(String nodeId) {
        return executionNodeRepository.findByNodeId(nodeId);
    }
    
    @Override
    public ExecutionNode registerNode(ExecutionNode node) {
        Optional<ExecutionNode> existingNode = executionNodeRepository.findByNodeId(node.getNodeId());
        if (existingNode.isPresent()) {
            ExecutionNode updatedNode = existingNode.get();
            updatedNode.setName(node.getName());
            updatedNode.setHost(node.getHost());
            updatedNode.setPort(node.getPort());
            updatedNode.setStatus("ONLINE");
            updatedNode.setOsInfo(node.getOsInfo());
            updatedNode.setCpuInfo(node.getCpuInfo());
            updatedNode.setMemoryInfo(node.getMemoryInfo());
            updatedNode.setEndpointType(node.getEndpointType());
            updatedNode.setLastHeartbeat(LocalDateTime.now());
            return executionNodeRepository.save(updatedNode);
        } else {
            node.setStatus("ONLINE");
            return executionNodeRepository.save(node);
        }
    }
    
    @Override
    public ExecutionNode updateNodeStatus(String nodeId, String status) {
        Optional<ExecutionNode> nodeOptional = executionNodeRepository.findByNodeId(nodeId);
        if (nodeOptional.isPresent()) {
            ExecutionNode node = nodeOptional.get();
            node.setStatus(status);
            return executionNodeRepository.save(node);
        }
        return null;
    }
    
    @Override
    public ExecutionNode updateHeartbeat(String nodeId) {
        Optional<ExecutionNode> nodeOptional = executionNodeRepository.findByNodeId(nodeId);
        if (nodeOptional.isPresent()) {
            ExecutionNode node = nodeOptional.get();
            node.setLastHeartbeat(LocalDateTime.now());
            if (!"ONLINE".equals(node.getStatus()) && !"BUSY".equals(node.getStatus())) {
                node.setStatus("ONLINE");
            }
            return executionNodeRepository.save(node);
        }
        return null;
    }
    
    @Override
    public void removeNode(String nodeId) {
        Optional<ExecutionNode> nodeOptional = executionNodeRepository.findByNodeId(nodeId);
        nodeOptional.ifPresent(executionNodeRepository::delete);
    }
    
    @Override
    public ExecutionNode getAvailableNode() {
        // 简单实现：返回第一个在线节点
        // 可以根据实际需求扩展为负载均衡算法
        List<ExecutionNode> nodes = executionNodeRepository.findAll();
        return nodes.stream()
                .filter(node -> "ONLINE".equals(node.getStatus()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<ExecutionNode> getOnlineExecutionNodes() {
        // 返回所有状态为ONLINE的执行节点
        List<ExecutionNode> nodes = executionNodeRepository.findAll();
        return nodes.stream()
                .filter(node -> "ONLINE".equals(node.getStatus()))
                .collect(Collectors.toList());
    }
}