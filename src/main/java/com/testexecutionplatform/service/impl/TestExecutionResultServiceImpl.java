package com.testexecutionplatform.service.impl;

import com.testexecutionplatform.model.TestExecutionResult;
import com.testexecutionplatform.repository.TestExecutionResultRepository;
import com.testexecutionplatform.service.TestExecutionResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestExecutionResultServiceImpl implements TestExecutionResultService {
    
    @Autowired
    private TestExecutionResultRepository testExecutionResultRepository;
    
    @Override
    public TestExecutionResult save(TestExecutionResult executionResult) {
        return testExecutionResultRepository.save(executionResult);
    }
    
    @Override
    public TestExecutionResult getById(Long id) {
        return testExecutionResultRepository.findById(id).orElse(null);
    }
    
    @Override
    public List<TestExecutionResult> getByScriptId(Long scriptId) {
        return testExecutionResultRepository.findByTestScript_IdOrderByStartTimeDesc(scriptId);
    }
    
    @Override
    public TestExecutionResult getLastByScriptId(Long scriptId) {
        List<TestExecutionResult> results = testExecutionResultRepository.findByTestScript_IdOrderByStartTimeDesc(scriptId);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void delete(Long id) {
        testExecutionResultRepository.deleteById(id);
    }
}
