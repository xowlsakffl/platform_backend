package com.platform.application.category;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.domain.category.Category;
import com.platform.domain.category.CategoryAssignment;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryStatus;
import com.platform.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.platform.infrastructure.persistence.category.CategoryRepository;
import com.platform.infrastructure.persistence.category.CategoryUsageRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CategoryAssignmentServiceTests {

	private CategoryRepository categoryRepository;
	private CategoryUsageRepository categoryUsageRepository;
	private CategoryAssignmentRepository categoryAssignmentRepository;
	private CategoryAssignmentService service;

	@BeforeEach
	void setUp() {
		categoryRepository = mock(CategoryRepository.class);
		categoryUsageRepository = mock(CategoryUsageRepository.class);
		categoryAssignmentRepository = mock(CategoryAssignmentRepository.class);
		service = new CategoryAssignmentService(
			categoryRepository,
			categoryUsageRepository,
			categoryAssignmentRepository
		);
	}

	@Test
	void keepsTheExistingAssignmentWhenThePrimaryCategoryDoesNotChange() {
		Category category = selectableCategory(48L);
		CategoryAssignment assignment = mock(CategoryAssignment.class);
		when(assignment.category()).thenReturn(category);
		when(categoryAssignmentRepository.findByCategorizableTypeAndCategorizableId("PARTNER", 9L))
			.thenReturn(List.of(assignment));

		service.replacePrimary(CategoryAssignmentTarget.PARTNER, 9L, 48L);

		verify(assignment).changePrimary(true);
		verify(categoryAssignmentRepository, never())
			.deleteByCategorizableTypeAndCategorizableId("PARTNER", 9L);
		verify(categoryAssignmentRepository, never()).flush();
		verify(categoryAssignmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void flushesTheDeleteBeforeSavingAChangedPrimaryCategory() {
		Category category = selectableCategory(49L);
		when(categoryAssignmentRepository.findByCategorizableTypeAndCategorizableId("PARTNER", 9L))
			.thenReturn(List.of());

		service.replacePrimary(CategoryAssignmentTarget.PARTNER, 9L, 49L);

		InOrder order = inOrder(categoryAssignmentRepository);
		order.verify(categoryAssignmentRepository)
			.deleteByCategorizableTypeAndCategorizableId("PARTNER", 9L);
		order.verify(categoryAssignmentRepository).flush();
		order.verify(categoryAssignmentRepository).save(org.mockito.ArgumentMatchers.any(CategoryAssignment.class));
	}

	private Category selectableCategory(Long id) {
		Category category = mock(Category.class);
		when(category.id()).thenReturn(id);
		when(category.domain()).thenReturn(CategoryDomain.PARTNER);
		when(category.status()).thenReturn(CategoryStatus.ACTIVE);
		when(category.depth()).thenReturn(1);
		when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
		when(categoryUsageRepository.existsByUsageAndCategory_IdAndStatus(
			CategoryAssignmentTarget.PARTNER.usage(),
			id,
			CategoryStatus.ACTIVE
		)).thenReturn(true);
		return category;
	}
}
