package com.testexecutionplatform.controller;

import com.testexecutionplatform.model.ExecutionNode;
import com.testexecutionplatform.service.ExecutionNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/execution-nodes")
public class ExecutionNodeController {
    
    @Autowired
    private ExecutionNodeService executionNodeService;
    
    @GetMapping
    public ResponseEntity<List<ExecutionNode>> getAllExecutionNodes() {
        List<ExecutionNode> nodes = executionNodeService.getAllExecutionNodes();
        return new ResponseEntity<>(nodes, HttpStatus.OK);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ExecutionNode> getExecutionNodeById(@PathVariable Long id) {
        return executionNodeService.getExecutionNodeById(id)
                .map(node -> new ResponseEntity<>(node, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @PostMapping("/register")
    public ResponseEntity<ExecutionNode> registerNode(@RequestBody ExecutionNode node) {
        ExecutionNode registeredNode = executionNodeService.registerNode(node);
        return new ResponseEntity<>(registeredNode, HttpStatus.CREATED);
    }
    
    @PostMapping("/{nodeId}/heartbeat")
    public ResponseEntity<ExecutionNode> updateHeartbeat(@PathVariable String nodeId) {
        ExecutionNode updatedNode = executionNodeService.updateHeartbeat(nodeId);
        if (updatedNode != null) {
            return new ResponseEntity<>(updatedNode, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/{nodeId}/status")
    public ResponseEntity<ExecutionNode> updateNodeStatus(@PathVariable String nodeId, @RequestBody String status) {
        ExecutionNode updatedNode = executionNodeService.updateNodeStatus(nodeId, status);
        if (updatedNode != null) {
            return new ResponseEntity<>(updatedNode, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{nodeId}")
    public ResponseEntity<Void> removeNode(@PathVariable String nodeId) {
        executionNodeService.removeNode(nodeId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @GetMapping("/available")
    public ResponseEntity<ExecutionNode> getAvailableNode() {
        ExecutionNode availableNode = executionNodeService.getAvailableNode();
        if (availableNode != null) {
            return new ResponseEntity<>(availableNode, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }
}
