package com.testexecutionplatform.repository;

import com.testexecutionplatform.model.TestExecutionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestExecutionResultRepository extends JpaRepository<TestExecutionResult, Long> {
    // 根据脚本ID查询执行记录，按开始时间倒序排列
    List<TestExecutionResult> findByTestScript_IdOrderByStartTimeDesc(Long scriptId);
}
