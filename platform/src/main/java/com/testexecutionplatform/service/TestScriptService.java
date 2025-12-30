package com.testexecutionplatform.service;

import com.testexecutionplatform.model.TestScript;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TestScriptService {
    List<TestScript> getAllTestScripts();
    Page<TestScript> getTestScripts(Pageable pageable);
    TestScript getTestScriptById(Long id);
    TestScript createTestScript(TestScript testScript, MultipartFile file);
    TestScript updateTestScript(Long id, TestScript testScript, MultipartFile file);
    void deleteTestScript(Long id);
    Map<String, Object> executeTestScript(Long id);
}