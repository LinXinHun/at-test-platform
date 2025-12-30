package com.testexecutionplatform.model;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "test_execution_results")
public class TestExecutionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "script_id", nullable = false)
    private TestScript testScript;
    
    @Column(name = "status", nullable = false)
    private String status; // running, success, error
    
    @Column(name = "output", columnDefinition = "TEXT")
    private String output;
    
    @Column(name = "error", columnDefinition = "TEXT")
    private String error;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "execution_time")
    private Long executionTime; // 执行时间，单位毫秒
    
    @Column(name = "log_file_path")
    private String logFilePath;
    
    @PrePersist
    protected void onCreate() {
        startTime = LocalDateTime.now();
    }
}