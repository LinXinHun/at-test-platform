package com.testexecutionplatform.service;

import com.testexecutionplatform.model.TestExecutionResult;

import java.util.List;

public interface TestExecutionResultService {
    // 保存执行记录
    TestExecutionResult save(TestExecutionResult executionResult);
    
    // 根据ID获取执行记录
    TestExecutionResult getById(Long id);
    
    // 根据脚本ID获取所有执行记录
    List<TestExecutionResult> getByScriptId(Long scriptId);
    
    // 获取脚本最后一次执行记录
    TestExecutionResult getLastByScriptId(Long scriptId);
    
    // 删除执行记录
    void delete(Long id);
}