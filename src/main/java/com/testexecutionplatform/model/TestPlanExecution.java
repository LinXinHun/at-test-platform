package com.testexecutionplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "test_plan_executions")
public class TestPlanExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    private TestPlan testPlan;
    
    @ManyToOne
    @JoinColumn(name = "node_id", nullable = false)
    private ExecutionNode executionNode;
    
    @Column(nullable = false)
    private String status; // EXECUTING, SUCCESS, FAILURE
    
    @Column(name = "total_scripts")
    private Integer totalScripts;
    
    @Column(name = "success_scripts")
    private Integer successScripts;
    
    @Column(name = "failed_scripts")
    private Integer failedScripts;
    
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