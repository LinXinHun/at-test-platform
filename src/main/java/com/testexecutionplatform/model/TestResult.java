package com.testexecutionplatform.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "test_results")
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private TestExecutionTask task;
    
    @ManyToOne
    @JoinColumn(name = "script_id", nullable = false)
    private TestScript script;
    
    @Column(nullable = false)
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String output;
    
    private String error;
    
    private Long duration;
    
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        executedAt = LocalDateTime.now();
    }
}