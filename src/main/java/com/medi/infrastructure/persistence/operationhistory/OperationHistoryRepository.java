package com.medi.infrastructure.persistence.operationhistory;

import com.medi.domain.operationhistory.OperationHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {

	@EntityGraph(attributePaths = "changes")
	List<OperationHistory> findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(String targetType, Long targetId);
}
