package com.medi.infrastructure.persistence.category;

import com.medi.domain.category.CategoryAssignment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryAssignmentRepository extends JpaRepository<CategoryAssignment, Long> {

	boolean existsByCategory_Id(Long categoryId);

	void deleteByCategorizableTypeAndCategorizableId(String categorizableType, Long categorizableId);

	@EntityGraph(attributePaths = "category")
	List<CategoryAssignment> findByCategorizableTypeAndCategorizableId(String categorizableType, Long categorizableId);

	@EntityGraph(attributePaths = "category")
	List<CategoryAssignment> findByCategorizableTypeAndCategorizableIdIn(String categorizableType, Collection<Long> ids);
}
