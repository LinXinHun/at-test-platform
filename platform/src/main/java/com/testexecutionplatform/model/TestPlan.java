package com.testexecutionplatform.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "test_plans")
public class TestPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @ManyToMany
    @JoinTable(
        name = "test_plan_scripts",
        joinColumns = @JoinColumn(name = "plan_id"),
        inverseJoinColumns = @JoinColumn(name = "script_id")
    )
    private List<TestScript> scripts;
    
    @Transient // 这个字段不会被持久化到数据库
    private List<Long> scriptIds;
    
    @Column(name = "last_execution_status")
    private String lastExecutionStatus; // EXECUTING, SUCCESS, FAILURE
    
    @Column(name = "last_execution_time")
    private LocalDateTime lastExecutionTime;
    
    @ManyToOne
    @JoinColumn(name = "last_execution_node_id")
    private ExecutionNode lastExecutionNode;
    
    @Column(name = "execution_endpoint_type", nullable = false)
    private String executionEndpointType; // MiniApp, Web, App, Api
    
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