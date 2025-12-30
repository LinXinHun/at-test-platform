package com.testexecutionplatform.repository;

import com.testexecutionplatform.model.ExecutionNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutionNodeRepository extends JpaRepository<ExecutionNode, Long> {
    Optional<ExecutionNode> findByNodeId(String nodeId);
    ExecutionNode findByStatus(String status);
}
