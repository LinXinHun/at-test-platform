package com.testexecutionplatform.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "test_execution_tasks")
public class TestExecutionTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private TestPlan plan;
    
    @Column(nullable = false)
    private String status;
    
    private String priority;
    
    private String environment;
    
    private String executor;
    
    @Column(name = "execution_node_id")
    private String executionNodeId;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
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
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}