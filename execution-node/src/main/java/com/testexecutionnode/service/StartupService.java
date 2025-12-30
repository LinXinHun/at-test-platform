package com.testexecutionnode.service;

import com.testexecutionnode.config.NodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class StartupService implements ApplicationRunner {

    @Autowired
    private RegistrationService registrationService;

    @Override
    public void run(ApplicationArguments args) {
        // 应用启动时注册到平台
        registrationService.registerToPlatform();
    }
}