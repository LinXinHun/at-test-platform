package com.testexecutionplatform.controller;

import com.testexecutionplatform.model.TestScript;
import com.testexecutionplatform.service.TestScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/scripts")
public class TestScriptController {

    @Autowired
    private TestScriptService testScriptService;

    @Value("${project.root.dir}")
    private String projectRootDir;

    @GetMapping
    public ResponseEntity<Page<TestScript>> getAllTestScripts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TestScript> scripts = testScriptService.getTestScripts(pageable);
        return new ResponseEntity<>(scripts, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestScript> getTestScriptById(@PathVariable Long id) {
        TestScript script = testScriptService.getTestScriptById(id);
        if (script != null) {
            return new ResponseEntity<>(script, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestScript> createTestScript(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestPart("scriptFile") MultipartFile file,
            @RequestParam(value = "parameters", required = false) String parameters,
            @RequestParam("scriptType") String scriptType,
            @RequestParam("timeout") int timeout,
            @RequestParam("retryCount") int retryCount) {
        TestScript testScript = new TestScript();
        testScript.setName(name);
        testScript.setDescription(description);
        testScript.setParameters(parameters != null ? parameters : "");
        testScript.setScriptType(scriptType);
        testScript.setTimeout(timeout);
        testScript.setRetryCount(retryCount);
        
        TestScript createdScript = testScriptService.createTestScript(testScript, file);
        return new ResponseEntity<>(createdScript, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestScript> updateTestScript(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestPart("scriptFile") MultipartFile file,
            @RequestParam(value = "parameters", required = false) String parameters,
            @RequestParam("scriptType") String scriptType,
            @RequestParam("timeout") int timeout,
            @RequestParam("retryCount") int retryCount) {
        TestScript testScript = new TestScript();
        testScript.setId(id);
        testScript.setName(name);
        testScript.setDescription(description);
        testScript.setParameters(parameters != null ? parameters : "");
        testScript.setScriptType(scriptType);
        testScript.setTimeout(timeout);
        testScript.setRetryCount(retryCount);
        
        TestScript updatedScript = testScriptService.updateTestScript(id, testScript, file);
        if (updatedScript != null) {
            return new ResponseEntity<>(updatedScript, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestScript(@PathVariable Long id) {
        testScriptService.deleteTestScript(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeTestScript(@PathVariable Long id) {
        Map<String, Object> result = testScriptService.executeTestScript(id);
        if ((boolean) result.getOrDefault("success", false)) {
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadScriptFile(@RequestParam String filePath) {
        try {
            // 构建完整的文件路径
            String scriptsDir = projectRootDir + File.separator + "scripts";
            
            // 安全检查：移除可能存在的scripts/前缀，避免路径重复
            if (filePath.startsWith("scripts/")) {
                filePath = filePath.substring("scripts/".length());
            }
            
            String fullPath = scriptsDir + File.separator + filePath;
            File file = new File(fullPath);
            
            // 检查文件是否存在
            if (!file.exists()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            // 创建资源对象
            Resource resource = new FileSystemResource(file);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(file.getName(), StandardCharsets.UTF_8)
                    .build());
            
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}