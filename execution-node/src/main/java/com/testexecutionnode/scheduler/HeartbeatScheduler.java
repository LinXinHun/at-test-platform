package com.testexecutionnode.scheduler;

import com.testexecutionnode.config.NodeConfig;
import com.testexecutionnode.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatScheduler {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatScheduler.class);

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private NodeConfig nodeConfig;

    /**
     * 定期发送心跳
     */
    @Scheduled(fixedRateString = "${heartbeat.interval}")
    public void sendHeartbeat() {
        logger.info("Scheduling heartbeat sending with interval: {}ms", nodeConfig.getHeartbeatInterval());
        registrationService.sendHeartbeat();
    }
}
