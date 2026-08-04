package com.platform.infrastructure.persistence.category;

import com.platform.domain.category.CategoryStatus;
import com.platform.domain.category.CategoryUsage;
import com.platform.domain.category.CategoryUsageType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryUsageRepository extends JpaRepository<CategoryUsage, Long> {

	boolean existsByCategory_Id(Long categoryId);

	boolean existsByUsageAndCategory_IdAndStatus(
		CategoryUsageType usage,
		Long categoryId,
		CategoryStatus status
	);

	@EntityGraph(attributePaths = "category")
	Optional<CategoryUsage> findByUsageAndCategory_IdAndStatus(
		CategoryUsageType usage,
		Long categoryId,
		CategoryStatus status
	);

	@EntityGraph(attributePaths = "category")
	List<CategoryUsage> findByUsageAndStatusOrderBySortOrderAscIdAsc(
		CategoryUsageType usage,
		CategoryStatus status
	);
}
