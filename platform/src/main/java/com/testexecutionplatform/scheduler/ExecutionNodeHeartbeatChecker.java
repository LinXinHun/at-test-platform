package com.testexecutionplatform.scheduler;

import com.testexecutionplatform.model.ExecutionNode;
import com.testexecutionplatform.service.ExecutionNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行节点心跳检查器
 * 定期检查执行节点的心跳状态，将长时间没有发送心跳的节点标记为离线
 */
@Component
public class ExecutionNodeHeartbeatChecker {

    @Autowired
    private ExecutionNodeService executionNodeService;

    // 心跳超时时间（秒）
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 60;

    /**
     * 定期检查执行节点心跳
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void checkNodeHeartbeats() {
        List<ExecutionNode> allNodes = executionNodeService.getAllExecutionNodes();
        LocalDateTime now = LocalDateTime.now();

        for (ExecutionNode node : allNodes) {
            // 检查节点是否有心跳记录
            if (node.getLastHeartbeat() != null) {
                // 计算心跳时间差
                long secondsSinceLastHeartbeat = java.time.Duration.between(node.getLastHeartbeat(), now).getSeconds();

                // 如果超过超时时间且节点当前不是离线状态，则更新状态为离线
                if (secondsSinceLastHeartbeat > HEARTBEAT_TIMEOUT_SECONDS && !"OFFLINE".equals(node.getStatus())) {
                    executionNodeService.updateNodeStatus(node.getNodeId(), "OFFLINE");
                    System.out.println("节点 " + node.getNodeId() + " 因超过 " + HEARTBEAT_TIMEOUT_SECONDS + " 秒未发送心跳，已标记为离线");
                }
            } else {
                // 没有心跳记录的节点也标记为离线
                if (!"OFFLINE".equals(node.getStatus())) {
                    executionNodeService.updateNodeStatus(node.getNodeId(), "OFFLINE");
                    System.out.println("节点 " + node.getNodeId() + " 没有心跳记录，已标记为离线");
                }
            }
        }
    }
}