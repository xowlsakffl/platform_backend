package com.platform.application.category;

import com.platform.application.category.result.CategoryReferenceResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.category.Category;
import com.platform.domain.category.CategoryAssignment;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.category.CategoryStatus;
import com.platform.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.platform.infrastructure.persistence.category.CategoryRepository;
import com.platform.infrastructure.persistence.category.CategoryUsageRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryAssignmentService {

	private final CategoryRepository categoryRepository;
	private final CategoryUsageRepository categoryUsageRepository;
	private final CategoryAssignmentRepository categoryAssignmentRepository;

	public CategoryAssignmentService(
		CategoryRepository categoryRepository,
		CategoryUsageRepository categoryUsageRepository,
		CategoryAssignmentRepository categoryAssignmentRepository
	) {
		this.categoryRepository = categoryRepository;
		this.categoryUsageRepository = categoryUsageRepository;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
	}

	@Transactional(readOnly = true)
	public Category requireSelectable(CategoryAssignmentTarget target, Long categoryId) {
		if (categoryId == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Category id is required.");
		}
		Category category = categoryRepository.findById(categoryId)
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Category not found."));
		boolean availableForUsage = categoryUsageRepository.existsByUsageAndCategory_IdAndStatus(
			target.usage(),
			categoryId,
			CategoryStatus.ACTIVE
		);
		if (!target.accepts(category) || !availableForUsage) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "Category cannot be selected for " + target.code() + ".");
		}
		return category;
	}

	@Transactional
	public Category replacePrimary(CategoryAssignmentTarget target, Long targetId, Long categoryId) {
		Category category = requireSelectable(target, categoryId);
		categoryAssignmentRepository.deleteByCategorizableTypeAndCategorizableId(target.code(), targetId);
		categoryAssignmentRepository.save(new CategoryAssignment(target.code(), targetId, category, true));
		return category;
	}

	@Transactional
	public void deleteAll(CategoryAssignmentTarget target, Long targetId) {
		categoryAssignmentRepository.deleteByCategorizableTypeAndCategorizableId(target.code(), targetId);
	}

	@Transactional(readOnly = true)
	public boolean isAssigned(CategoryAssignmentTarget target, Long targetId, Long categoryId) {
		return categoryAssignmentRepository
			.existsByCategorizableTypeAndCategorizableIdAndCategory_Id(target.code(), targetId, categoryId);
	}

	@Transactional(readOnly = true)
	public List<CategoryReferenceResult> references(CategoryAssignmentTarget target, Long targetId) {
		return references(categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableId(target.code(), targetId));
	}

	@Transactional(readOnly = true)
	public Map<Long, List<CategoryReferenceResult>> referencesByTargetIds(
		CategoryAssignmentTarget target,
		Collection<Long> targetIds
	) {
		if (targetIds == null || targetIds.isEmpty()) {
			return Map.of();
		}
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableIdIn(target.code(), targetIds)
			.stream()
			.collect(Collectors.groupingBy(
				CategoryAssignment::categorizableId,
				LinkedHashMap::new,
				Collectors.collectingAndThen(Collectors.toList(), this::references)
			));
	}

	private List<CategoryReferenceResult> references(List<CategoryAssignment> assignments) {
		return assignments.stream()
			.sorted(Comparator
				.comparing(CategoryAssignment::primary).reversed()
				.thenComparing(assignment -> assignment.category().sortOrder())
				.thenComparing(assignment -> assignment.category().id()))
			.map(assignment -> reference(assignment.category(), assignment.primary()))
			.toList();
	}

	private CategoryReferenceResult reference(Category category, boolean primary) {
		return new CategoryReferenceResult(
			category.id(),
			category.name(),
			category.code(),
			category.fullPath(),
			category.parentId(),
			category.depth(),
			primary
		);
	}
}
