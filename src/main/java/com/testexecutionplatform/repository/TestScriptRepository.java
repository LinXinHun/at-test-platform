package com.testexecutionplatform.repository;

import com.testexecutionplatform.model.TestScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestScriptRepository extends JpaRepository<TestScript, Long> {
}
