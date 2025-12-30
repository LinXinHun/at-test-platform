package com.testexecutionplatform.repository;

import com.testexecutionplatform.model.TestPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestPlanRepository extends JpaRepository<TestPlan, Long> {
    // 加载测试计划时同时加载关联的脚本集合
    @Override
    @EntityGraph(attributePaths = {"scripts"})
    Optional<TestPlan> findById(Long id);
}