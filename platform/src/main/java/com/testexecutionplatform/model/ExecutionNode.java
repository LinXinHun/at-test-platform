package com.testexecutionplatform.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "execution_nodes")
public class ExecutionNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String nodeId;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private String host;
    
    private Integer port;
    
    @Column(nullable = false)
    private String status; // ONLINE, BUSY, OFFLINE
    
    private String osInfo;
    
    private String cpuInfo;
    
    private String memoryInfo;
    
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastHeartbeat = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}