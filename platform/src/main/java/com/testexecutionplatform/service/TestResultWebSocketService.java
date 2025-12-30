package com.testexecutionplatform.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class TestResultWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public TestResultWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendTestResultUpdate(String message) {
        // 广播测试结果更新到所有订阅者
        messagingTemplate.convertAndSend("/topic/testResults", message);
    }

    public void sendTestResultUpdateToUser(String userId, String message) {
        // 发送测试结果更新到指定用户
        messagingTemplate.convertAndSendToUser(userId, "/queue/testResults", message);
    }
}
