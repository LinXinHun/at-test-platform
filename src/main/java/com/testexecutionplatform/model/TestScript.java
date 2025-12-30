package com.testexecutionplatform.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "test_scripts")
public class TestScript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(columnDefinition = "TEXT", name = "script_content")
    private String scriptContent;
    
    @Column(name = "parameters")
    private String parameters;
    
    @Column(name = "script_type")
    private String scriptType;
    
    @Column(name = "timeout")
    private Integer timeout;
    
    @Column(name = "retry_count")
    private Integer retryCount;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}