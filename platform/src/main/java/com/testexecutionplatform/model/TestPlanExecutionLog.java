package com.testexecutionplatform.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "test_plan_execution_logs")
public class TestPlanExecutionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "execution_id", nullable = false)
    private TestPlanExecution execution;
    
    @ManyToOne
    @JoinColumn(name = "script_id", nullable = false)
    private TestScript testScript;
    
    @Column(nullable = false)
    private String status; // EXECUTING, SUCCESS, FAILURE
    
    // 修改result字段为TEXT类型，支持更长的文本内容
    @Column(columnDefinition = "TEXT")
    private String result;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "execution_time")
    private Long executionTime; // in milliseconds
    
    @Column(columnDefinition = "TEXT")
    private String logContent;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        startTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}