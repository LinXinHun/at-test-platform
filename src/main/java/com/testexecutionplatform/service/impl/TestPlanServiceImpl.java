package com.testexecutionplatform.service.impl;

import com.testexecutionplatform.model.TestPlan;
import com.testexecutionplatform.model.TestScript;
import com.testexecutionplatform.repository.TestPlanRepository;
import com.testexecutionplatform.service.TestPlanService;
import com.testexecutionplatform.service.TestScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TestPlanServiceImpl implements TestPlanService {

    @Autowired
    private TestPlanRepository testPlanRepository;

    @Autowired
    private TestScriptService testScriptService;

    @Override
    public List<TestPlan> getAllTestPlans() {
        return testPlanRepository.findAll();
    }

    @Override
    public Page<TestPlan> getTestPlans(Pageable pageable) {
        return testPlanRepository.findAll(pageable);
    }

    @Override
    public TestPlan getTestPlanById(Long id) {
        // 使用默认的findById方法，由于在Repository中重写了该方法并添加了@EntityGraph注解，会同时加载scripts集合
        Optional<TestPlan> optionalTestPlan = testPlanRepository.findById(id);
        return optionalTestPlan.orElse(null);
    }

    @Override
    public TestPlan createTestPlan(TestPlan testPlan) {
        // 处理前端传递的scriptIds参数
        if (testPlan.getScriptIds() != null && !testPlan.getScriptIds().isEmpty()) {
            // 根据scriptIds获取TestScript对象
            List<TestScript> scripts = testPlan.getScriptIds().stream()
                    .map(testScriptService::getTestScriptById)
                    .filter(script -> script != null)
                    .collect(Collectors.toList());
            
            testPlan.setScripts(scripts);
        }
        
        return testPlanRepository.save(testPlan);
    }

    @Override
    public TestPlan updateTestPlan(Long id, TestPlan testPlan) {
        if (testPlanRepository.existsById(id)) {
            testPlan.setId(id);
            
            // 处理前端传递的scriptIds参数
            if (testPlan.getScriptIds() != null) {
                // 根据scriptIds获取TestScript对象
                List<TestScript> scripts = testPlan.getScriptIds().stream()
                        .map(testScriptService::getTestScriptById)
                        .filter(script -> script != null)
                        .collect(Collectors.toList());
                
                testPlan.setScripts(scripts);
            }
            
            return testPlanRepository.save(testPlan);
        }
        return null;
    }

    @Override
    public void deleteTestPlan(Long id) {
        testPlanRepository.deleteById(id);
    }
}