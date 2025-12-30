package com.testexecutionplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Entity
@Table(name = "test_reports")
public class TestReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "execution_id", nullable = true)
    @JsonIgnore
    private TestPlanExecution execution;
    
    @Column(name = "execution_id", insertable = false, updatable = false)
    @JsonProperty("executionId")
    private Long executionId;
    
    @Column(name = "plan_name", nullable = false)
    private String planName;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "total_scripts")
    private Integer totalScripts;
    
    @Column(name = "success_scripts")
    private Integer successScripts;
    
    @Column(name = "failed_scripts")
    private Integer failedScripts;
    
    @Column(name = "pass_rate")
    private Double passRate;
    
    @Column(name = "generation_time", nullable = false)
    private LocalDateTime generationTime;
    
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    
    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData; // JSON格式，用于存储图表数据
    
    @Column(nullable = false)
    private String status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        generationTime = LocalDateTime.now();
        generatedAt = LocalDateTime.now();
        name = "Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}