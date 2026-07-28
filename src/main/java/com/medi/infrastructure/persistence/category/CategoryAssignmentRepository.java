package com.medi.infrastructure.persistence.category;

import com.medi.domain.category.CategoryAssignment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryAssignmentRepository extends JpaRepository<CategoryAssignment, Long> {

	void deleteByCategorizableTypeAndCategorizableId(String categorizableType, Long categorizableId);

	List<CategoryAssignment> findByCategorizableTypeAndCategorizableId(String categorizableType, Long categorizableId);

	List<CategoryAssignment> findByCategorizableTypeAndCategorizableIdIn(String categorizableType, Collection<Long> ids);
}
