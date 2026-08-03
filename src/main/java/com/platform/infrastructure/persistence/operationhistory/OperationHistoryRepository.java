package com.platform.infrastructure.persistence.operationhistory;

import com.platform.domain.operationhistory.OperationHistory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {

	@EntityGraph(attributePaths = "changes")
	List<OperationHistory> findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(String targetType, Long targetId);

	Page<OperationHistory> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);
}
