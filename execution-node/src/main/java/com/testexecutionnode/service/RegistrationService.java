package com.testexecutionnode.service;

import com.testexecutionnode.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class RegistrationService {
    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    @Autowired
    private NodeConfig nodeConfig;

    @Autowired
    private RestTemplate restTemplate;

    private String platformServerUrl;
    private String nodeId;
    private String nodeName;
    private String nodeHost;
    private int nodePort;

    @PostConstruct
    public void init() {
        platformServerUrl = nodeConfig.getPlatformServerUrl();
        nodeId = nodeConfig.getNodeId();
        nodeName = nodeConfig.getNodeName();
        nodeHost = nodeConfig.getNodeHost();
        nodePort = nodeConfig.getNodePort();

        // 注册到平台
        registerToPlatform();
    }

    public void registerToPlatform() {
        try {
            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("nodeId", nodeId);
            nodeInfo.put("name", nodeName);
            nodeInfo.put("host", nodeHost);
            nodeInfo.put("port", nodePort);
            nodeInfo.put("osInfo", System.getProperty("os.name") + " " + System.getProperty("os.version"));
            nodeInfo.put("cpuInfo", Runtime.getRuntime().availableProcessors() + " cores");
            nodeInfo.put("memoryInfo", (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");

            String url = platformServerUrl + "/api/execution-nodes/register";
            logger.info("Registering to platform at {} with nodeId: {}", url, nodeId);
            restTemplate.postForEntity(url, nodeInfo, Void.class);

            logger.info("Successfully registered to platform");
        } catch (Exception e) {
            logger.error("Failed to register to platform: " + e.getMessage());
        }
    }

    public void sendHeartbeat() {
        try {
            String url = platformServerUrl + "/api/execution-nodes/" + nodeId + "/heartbeat";
            logger.info("Sending heartbeat to {} with nodeId: {}", url, nodeId);
            restTemplate.postForEntity(url, null, Void.class);
            logger.info("Heartbeat sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send heartbeat: " + e.getMessage());
            // 如果心跳失败，可能是节点已从平台中移除，尝试重新注册
            registerToPlatform();
        }
    }

    public void updateStatus(String status) {
        try {
            String url = platformServerUrl + "/api/execution-nodes/" + nodeId + "/status";
            Map<String, String> statusMap = new HashMap<>();
            statusMap.put("status", status);
            restTemplate.put(url, statusMap);
            logger.info("Status updated to {}", status);
        } catch (Exception e) {
            logger.error("Failed to update status: " + e.getMessage());
        }
    }
}