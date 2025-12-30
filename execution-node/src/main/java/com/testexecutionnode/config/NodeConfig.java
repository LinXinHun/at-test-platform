package com.testexecutionnode.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NodeConfig {

    @Value("${platform.server.url}")
    private String platformServerUrl;

    @Value("${node.id}")
    private String nodeId;

    @Value("${node.name}")
    private String nodeName;

    @Value("${node.host}")
    private String nodeHost;

    @Value("${node.port}")
    private int nodePort;

    @Value("${heartbeat.interval}")
    private long heartbeatInterval;
    
    @Value("${temp.script.directory}")
    private String tempScriptDirectory;

    public String getPlatformServerUrl() {
        return platformServerUrl;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeHost() {
        return nodeHost;
    }

    public int getNodePort() {
        return nodePort;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public String getTempScriptDirectory() {
        return tempScriptDirectory;
    }
}