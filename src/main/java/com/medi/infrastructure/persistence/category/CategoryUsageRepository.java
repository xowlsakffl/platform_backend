package com.medi.infrastructure.persistence.category;

import com.medi.domain.category.CategoryStatus;
import com.medi.domain.category.CategoryUsage;
import com.medi.domain.category.CategoryUsageType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryUsageRepository extends JpaRepository<CategoryUsage, Long> {

	@EntityGraph(attributePaths = "category")
	List<CategoryUsage> findByUsageAndStatusOrderBySortOrderAscIdAsc(
		CategoryUsageType usage,
		CategoryStatus status
	);

	boolean existsByCategory_Id(Long categoryId);

	long countByUsageAndStatusAndCategory_IdIn(
		CategoryUsageType usage,
		CategoryStatus status,
		Collection<Long> categoryIds
	);
}
