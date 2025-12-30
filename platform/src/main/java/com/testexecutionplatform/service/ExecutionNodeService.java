package com.testexecutionplatform.service;

import com.testexecutionplatform.model.ExecutionNode;

import java.util.List;
import java.util.Optional;

public interface ExecutionNodeService {
    List<ExecutionNode> getAllExecutionNodes();
    Optional<ExecutionNode> getExecutionNodeById(Long id);
    Optional<ExecutionNode> getExecutionNodeByNodeId(String nodeId);
    ExecutionNode registerNode(ExecutionNode node);
    ExecutionNode updateNodeStatus(String nodeId, String status);
    ExecutionNode updateHeartbeat(String nodeId);
    void removeNode(String nodeId);
    ExecutionNode getAvailableNode();
    List<ExecutionNode> getOnlineExecutionNodes();
}