package com.testexecutionplatform.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class TestResultWebSocketController {

    @MessageMapping("/testResult")
    @SendTo("/topic/testResults")
    public String sendTestResult(String message) {
        // 处理测试结果消息并广播
        return "New test result: " + message;
    }
}
