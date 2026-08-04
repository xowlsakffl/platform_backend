package com.platform.infrastructure.persistence.category;

import com.platform.domain.category.CategoryAssignment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryAssignmentRepository extends JpaRepository<CategoryAssignment, Long> {

	boolean existsByCategory_Id(Long categoryId);

	boolean existsByCategorizableTypeAndCategorizableIdAndCategory_Id(
		String categorizableType,
		Long categorizableId,
		Long categoryId
	);

	void deleteByCategorizableTypeAndCategorizableId(String categorizableType, Long categorizableId);

	@EntityGraph(attributePaths = "category")
	List<CategoryAssignment> findByCategorizableTypeAndCategorizableId(String categorizableType, Long categorizableId);

	@EntityGraph(attributePaths = "category")
	List<CategoryAssignment> findByCategorizableTypeAndCategorizableIdIn(String categorizableType, Collection<Long> ids);
}
