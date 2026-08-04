package com.platform.application.category;

import com.platform.application.auth.PermissionService;
import com.platform.application.category.result.CategoryReferenceResult;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.category.Category;
import com.platform.domain.category.CategoryAssignmentTarget;
import com.platform.domain.category.CategoryStatus;
import com.platform.domain.category.CategoryUsageType;
import com.platform.infrastructure.persistence.category.CategoryUsageRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryForPartnerService {

	private final PermissionService permissionService;
	private final CategoryAssignmentService categoryAssignmentService;
	private final CategoryUsageRepository categoryUsageRepository;

	public CategoryForPartnerService(
		PermissionService permissionService,
		CategoryAssignmentService categoryAssignmentService,
		CategoryUsageRepository categoryUsageRepository
	) {
		this.permissionService = permissionService;
		this.categoryAssignmentService = categoryAssignmentService;
		this.categoryUsageRepository = categoryUsageRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoryReferenceResult> selector(
		AuthenticatedActor actor,
		CategoryUsageType usage,
		Long parentId,
		String query
	) {
		permissionService.requireActor(actor, AccountActorType.PARTNER);
		Set<Long> allowedParentIds = allowedParentIds(actor, usage);
		String keyword = normalize(query);

		return categoryUsageRepository
			.findByUsageAndStatusOrderBySortOrderAscIdAsc(usage, CategoryStatus.ACTIVE)
			.stream()
			.map(categoryUsage -> categoryUsage.category())
			.filter(category -> parentId == null || parentId.equals(category.parentId()))
			.filter(category -> usage != CategoryUsageType.PARTNER_OPTION_CATEGORY
				|| category.parentId() != null && allowedParentIds.contains(category.parentId()))
			.filter(category -> keyword == null || matches(category, keyword))
			.map(category -> new CategoryReferenceResult(
				category.id(),
				category.name(),
				category.code(),
				category.fullPath(),
				category.parentId(),
				category.depth(),
				false
			))
			.toList();
	}

	private Set<Long> allowedParentIds(AuthenticatedActor actor, CategoryUsageType usage) {
		if (usage != CategoryUsageType.PARTNER_OPTION_CATEGORY) {
			return Set.of();
		}
		return new HashSet<>(categoryAssignmentService
			.references(CategoryAssignmentTarget.PARTNER, actor.partnerId())
			.stream()
			.map(CategoryReferenceResult::id)
			.toList());
	}

	private boolean matches(Category category, String keyword) {
		return contains(category.name(), keyword)
			|| contains(category.code(), keyword)
			|| contains(category.fullPath(), keyword);
	}

	private boolean contains(String value, String keyword) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
