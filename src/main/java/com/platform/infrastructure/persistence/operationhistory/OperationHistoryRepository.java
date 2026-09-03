package com.platform.infrastructure.persistence.operationhistory;

import com.platform.domain.operationhistory.OperationHistory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {

	@EntityGraph(attributePaths = "changes")
	List<OperationHistory> findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(String targetType, Long targetId);

	Page<OperationHistory> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);

	@Query("""
		select history
		from OperationHistory history
		where (history.targetType = :accountTargetType and history.targetId = :accountId)
		   or (history.actorType = :partnerActorType and history.actorId = :accountId)
		""")
	Page<OperationHistory> findAllForPartnerAccountHistory(
		String accountTargetType,
		Long accountId,
		String partnerActorType,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "changes")
	@Query("select distinct history from OperationHistory history where history.id in :ids")
	List<OperationHistory> findWithChangesByIdIn(List<Long> ids);

	@Query("""
		select history.id
		from OperationHistory history
		where history.targetType = :targetType
		  and history.targetId = :targetId
		  and exists (
			select change.id
			from OperationHistoryChange change
			where change.operationHistory = history
			  and change.fieldKey = :fieldKey
			  and change.afterValue = :afterValue
		  )
		order by history.createdAt desc, history.id desc
		""")
	List<Long> findLatestIdsByChange(
		String targetType,
		Long targetId,
		String fieldKey,
		String afterValue,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "changes")
	@Query("select history from OperationHistory history where history.id = :id")
	java.util.Optional<OperationHistory> findWithChangesById(Long id);
}
