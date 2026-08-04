package com.platform.application.category;

import com.platform.application.auth.PermissionService;
import com.platform.application.category.command.CreateCategoryCommand;
import com.platform.application.category.command.UpdateCategoryCommand;
import com.platform.application.category.query.SearchCategoriesQuery;
import com.platform.application.category.query.SelectCategoriesQuery;
import com.platform.application.category.result.CategoryDeletedResult;
import com.platform.application.category.result.CategoryResult;
import com.platform.application.media.MediaCollectionPolicy;
import com.platform.application.media.MediaCommandService;
import com.platform.application.media.MediaLifecycleService;
import com.platform.application.media.MediaReadService;
import com.platform.application.media.result.MediaResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.security.AccessPermissions;
import com.platform.common.web.PaginatedResponse;
import com.platform.domain.account.AccountActorType;
import com.platform.domain.category.Category;
import com.platform.domain.category.CategoryDomain;
import com.platform.domain.category.CategoryGroup;
import com.platform.domain.category.CategoryStatus;
import com.platform.domain.category.CategoryUsage;
import com.platform.domain.operationhistory.OperationHistory;
import com.platform.domain.media.MediaOwnerType;
import com.platform.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.platform.infrastructure.persistence.category.CategoryRepository;
import com.platform.infrastructure.persistence.category.CategoryUsageRepository;
import com.platform.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryForStaffService {

	private static final int MAX_DEPTH = 3;

	private final PermissionService permissionService;
	private final CategoryRepository categoryRepository;
	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final CategoryUsageRepository categoryUsageRepository;
	private final OperationHistoryRepository operationHistoryRepository;
	private final MediaCommandService mediaCommandService;
	private final MediaReadService mediaReadService;
	private final MediaLifecycleService mediaLifecycleService;

	public CategoryForStaffService(
		PermissionService permissionService,
		CategoryRepository categoryRepository,
		CategoryAssignmentRepository categoryAssignmentRepository,
		CategoryUsageRepository categoryUsageRepository,
		OperationHistoryRepository operationHistoryRepository,
		MediaCommandService mediaCommandService,
		MediaReadService mediaReadService,
		MediaLifecycleService mediaLifecycleService
	) {
		this.permissionService = permissionService;
		this.categoryRepository = categoryRepository;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
		this.categoryUsageRepository = categoryUsageRepository;
		this.operationHistoryRepository = operationHistoryRepository;
		this.mediaCommandService = mediaCommandService;
		this.mediaReadService = mediaReadService;
		this.mediaLifecycleService = mediaLifecycleService;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<CategoryResult> list(AuthenticatedActor actor, SearchCategoriesQuery condition) {
		permissionService.requireStaffPermission(actor, AccessPermissions.CATEGORY_MANAGE);
		Page<Category> page = categoryRepository.findAll(
			specification(condition),
			PageRequest.of(
				Math.max(condition.page(), 1) - 1,
				clamp(condition.perPage(), 1, 100),
				sort(condition.sort(), condition.direction())
			)
		);
		List<Category> domainCategories = categoryRepository.findAllByDomain(
			condition.domain(),
			Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))
		);
		CategoryTreeSummary summary = summarize(domainCategories);
		Map<Long, Category> categoryById = domainCategories.stream()
			.collect(Collectors.toMap(Category::id, Function.identity()));
		Map<Long, List<Category>> childrenByParentId = childrenByParentId(domainCategories);
		boolean includeParent = condition.include().contains("parent");
		boolean includeChildren = condition.include().contains("children");
		Map<Long, MediaResult> icons = icons(includeParent || includeChildren ? domainCategories : page.getContent());

		return PaginatedResponse.from(page, category -> toResult(
			category,
			summary,
			true,
			icons.get(category.id()),
			includeParent ? simpleResult(categoryById.get(category.parentId()), summary, icons) : null,
			includeChildren ? childResults(category, childrenByParentId, summary, icons) : null
		));
	}

	@Transactional(readOnly = true)
	public List<CategoryResult> selector(AuthenticatedActor actor, SelectCategoriesQuery condition) {
		permissionService.requireActor(actor, AccountActorType.STAFF);
		List<Category> all = categoryRepository.findAllByDomain(
			condition.domain(),
			Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))
		);
		Map<Long, Integer> usageOrder = condition.usage() == null
			? Map.of()
			: categoryUsageRepository
				.findByUsageAndStatusOrderBySortOrderAscIdAsc(condition.usage(), CategoryStatus.ACTIVE)
				.stream()
				.collect(Collectors.toMap(
					usage -> usage.category().id(),
					CategoryUsage::sortOrder,
					(first, ignored) -> first,
					LinkedHashMap::new
				));
		Long parentId = resolveParentId(condition);
		boolean parentFilterRequested = condition.parentId() != null || hasText(condition.parentCode());
		if (parentFilterRequested && parentId == null) {
			return List.of();
		}
		String keyword = normalizeKeyword(condition.q());
		Set<CategoryStatus> statuses = condition.status() == null || condition.status().isEmpty()
			? Set.of(CategoryStatus.ACTIVE)
			: Set.copyOf(condition.status());
		List<Category> selected = all.stream()
			.filter(category -> condition.usage() == null || usageOrder.containsKey(category.id()))
			.filter(category -> statuses.contains(category.status()))
			.filter(category -> condition.groupCode() == null || category.groupCode() == condition.groupCode())
			.filter(category -> condition.menuVisible() == null || category.menuVisible() == condition.menuVisible())
			.filter(category -> condition.depth() == null || category.depth() == condition.depth())
			.filter(category -> matchesParent(
				category,
				parentId,
				parentFilterRequested,
				condition.depth() != null || condition.usage() != null,
				keyword
			))
			.filter(category -> matchesKeyword(category, keyword))
			.sorted(condition.usage() == null
				? selectorComparator(condition)
				: Comparator
					.comparingInt((Category category) -> usageOrder.get(category.id()))
					.thenComparing(Category::id))
			.toList();
		if (keyword != null) {
			selected = selected.stream().limit(clamp(condition.perPage(), 1, 100)).toList();
		}
		CategoryTreeSummary summary = summarize(all);
		Map<Long, MediaResult> icons = icons(selected);

		return selected.stream().map(category -> toResult(category, summary, false, icons.get(category.id()))).toList();
	}

	@Transactional(readOnly = true)
	public CategoryResult get(AuthenticatedActor actor, Long id, List<String> include) {
		permissionService.requireStaffPermission(actor, AccessPermissions.CATEGORY_MANAGE);
		Category category = findCategory(id);
		List<Category> domainCategories = categoryRepository.findAllByDomain(
			category.domain(),
			Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))
		);
		CategoryTreeSummary summary = summarize(domainCategories);
		Map<Long, Category> categoryById = domainCategories.stream()
			.collect(Collectors.toMap(Category::id, Function.identity()));
		Map<Long, MediaResult> icons = icons(domainCategories);
		return toResult(
			category,
			summary,
			true,
			icons.get(category.id()),
			include.contains("parent") ? simpleResult(categoryById.get(category.parentId()), summary, icons) : null,
			include.contains("children")
				? childResults(category, childrenByParentId(domainCategories), summary, icons)
				: null
		);
	}

	@Transactional
	public CategoryResult create(AuthenticatedActor actor, CreateCategoryCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.CATEGORY_MANAGE);
		requireMutableDomain(command.domain());
		String name = required(command.name(), "카테고리명은 필수입니다.");
		Category parent = findParent(command.domain(), command.parentId());
		if (parent != null && parent.depth() >= MAX_DEPTH) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "3단계 카테고리 아래에는 카테고리를 추가할 수 없습니다.");
		}
		String code = trimToNull(command.code());
		ensureCodeAvailable(command.domain(), code, null);
		CategoryGroup groupCode = resolveGroup(command.domain(), parent, command.groupCode());
		ensureSiblingNameAvailable(command.domain(), command.parentId(), groupCode, name, null);
		byte depth = (byte) (parent == null ? 1 : parent.depth() + 1);
		String fullPath = parent == null ? name : parent.fullPath() + " > " + name;
		Category category = categoryRepository.save(new Category(
			command.domain(),
			parent,
			depth,
			groupCode,
			name,
			code,
			fullPath,
			command.sortOrder() == null ? 0 : command.sortOrder(),
			command.status() == null ? CategoryStatus.ACTIVE : command.status(),
			command.menuVisible() == null || command.menuVisible()
		));
		if (command.icon() != null) {
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.CATEGORY,
				category.id(),
				MediaCollectionPolicy.CATEGORY_ICON,
				command.icon(),
				null,
				false
			);
		}
		recordHistory(actor, category, "CREATED", null, Map.of(), capture(category));

		return toResult(category, summarize(command.domain()), true, icon(category.id()));
	}

	@Transactional
	public CategoryResult update(AuthenticatedActor actor, Long id, UpdateCategoryCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.CATEGORY_MANAGE);
		Category category = findLockedCategory(id);
		requireDefinitionFieldsMutable(category, command);
		Map<String, String> before = capture(category);
		String oldPath = category.fullPath();
		CategoryGroup oldGroup = category.groupCode();
		String name = command.name() == null ? category.name() : required(command.name(), "카테고리명은 비워둘 수 없습니다.");
		String code = command.codeSpecified() ? trimToNull(command.code()) : category.code();
		ensureCodeAvailable(category.domain(), code, category.id());
		CategoryGroup requestedGroup = command.groupCodeSpecified() ? command.groupCode() : oldGroup;
		CategoryGroup groupCode = resolveGroup(category.domain(), category.parent(), requestedGroup);
		ensureSiblingNameAvailable(category.domain(), category.parentId(), groupCode, name, category.id());
		String fullPath = category.parent() == null ? name : category.parent().fullPath() + " > " + name;

		category.update(
			name,
			code,
			fullPath,
			groupCode,
			command.sortOrder() == null ? category.sortOrder() : command.sortOrder(),
			command.status() == null ? category.status() : command.status(),
			command.menuVisible() == null ? category.menuVisible() : command.menuVisible()
		);
		if (!Objects.equals(oldPath, fullPath) || oldGroup != groupCode) {
			List<Category> descendants = categoryRepository.findByDomainAndGroupCodeAndFullPathStartingWithOrderByDepthAsc(
				category.domain(),
				oldGroup,
				oldPath + " > "
			);
			for (Category descendant : descendants) {
				String nextPath = fullPath + descendant.fullPath().substring(oldPath.length());
				descendant.updateInheritedValues(nextPath, groupCode);
			}
		}
		if (command.icon() != null) {
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.CATEGORY,
				category.id(),
				MediaCollectionPolicy.CATEGORY_ICON,
				command.icon(),
				null,
				false
			);
		}
		recordHistory(actor, category, "UPDATED", null, before, capture(category));

		return toResult(category, summarize(category.domain()), true, icon(category.id()));
	}

	@Transactional
	public CategoryDeletedResult delete(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, AccessPermissions.CATEGORY_MANAGE);
		Category category = findLockedCategory(id);
		requireMutableDomain(category.domain());
		if (categoryRepository.existsByParent_Id(id)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "하위 카테고리가 있어 삭제할 수 없습니다.");
		}
		if (categoryAssignmentRepository.existsByCategory_Id(id)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "연결된 데이터가 있어 삭제할 수 없습니다.");
		}
		if (categoryUsageRepository.existsByCategory_Id(id)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "사용 중인 카테고리라 삭제할 수 없습니다.");
		}
		recordHistory(actor, category, "DELETED", null, capture(category), Map.of());
		mediaLifecycleService.softDeleteOwnedMedia(MediaOwnerType.CATEGORY, category.id());
		categoryRepository.delete(category);
		return new CategoryDeletedResult(id);
	}

	private Specification<Category> specification(SearchCategoriesQuery condition) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("domain"), condition.domain()));
			String keyword = normalizeKeyword(condition.q());
			if (keyword != null) {
				String like = "%" + keyword + "%";
				predicates.add(criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), like),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), like),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("fullPath")), like)
				));
			}
			if (condition.parentId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("parent").get("id"), condition.parentId()));
			}
			if (condition.depth() != null) {
				predicates.add(criteriaBuilder.equal(root.get("depth"), condition.depth()));
			} else if (condition.parentId() == null && keyword == null) {
				predicates.add(criteriaBuilder.isNull(root.get("parent")));
			}
			if (condition.groupCode() != null) {
				predicates.add(criteriaBuilder.equal(root.get("groupCode"), condition.groupCode()));
			}
			if (condition.status() != null && !condition.status().isEmpty()) {
				predicates.add(root.get("status").in(condition.status()));
			}
			if (condition.menuVisible() != null) {
				predicates.add(criteriaBuilder.equal(root.get("menuVisible"), condition.menuVisible()));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private boolean matchesParent(
		Category category,
		Long parentId,
		boolean parentFilterRequested,
		boolean depthFilterRequested,
		String keyword
	) {
		if (parentFilterRequested && !Objects.equals(category.parentId(), parentId)) {
			return false;
		}
		if (!parentFilterRequested && !depthFilterRequested && keyword == null && category.depth() != 1) {
			return false;
		}
		return true;
	}

	private boolean matchesKeyword(Category category, String keyword) {
		if (keyword == null) {
			return true;
		}
		return contains(category.name(), keyword)
			|| contains(category.code(), keyword)
			|| contains(category.fullPath(), keyword);
	}

	private Comparator<Category> selectorComparator(SelectCategoriesQuery condition) {
		Comparator<Category> comparator = switch (condition.sort()) {
			case "id" -> Comparator.comparing(Category::id);
			case "name" -> Comparator.comparing(Category::name, String.CASE_INSENSITIVE_ORDER);
			case "depth" -> Comparator.comparingInt(Category::depth);
			case "group_code" -> Comparator.comparing(
				category -> category.groupCode() == null ? "" : category.groupCode().name()
			);
			case "status" -> Comparator.comparing(category -> category.status().name());
			default -> Comparator.comparingInt(Category::sortOrder);
		};
		comparator = comparator.thenComparing(Category::id);
		return "desc".equalsIgnoreCase(condition.direction()) ? comparator.reversed() : comparator;
	}

	private Sort sort(String field, String direction) {
		String property = switch (field) {
			case "id" -> "id";
			case "name" -> "name";
			case "depth" -> "depth";
			case "group_code" -> "groupCode";
			case "status" -> "status";
			case "created_at" -> "createdAt";
			case "updated_at" -> "updatedAt";
			default -> "sortOrder";
		};
		Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(new Sort.Order(sortDirection, property), Sort.Order.asc("id"));
	}

	private CategoryResult toResult(
		Category category,
		CategoryTreeSummary summary,
		boolean includeCounts,
		MediaResult icon
	) {
		return toResult(category, summary, includeCounts, icon, null, null);
	}

	private CategoryResult toResult(
		Category category,
		CategoryTreeSummary summary,
		boolean includeCounts,
		MediaResult icon,
		CategoryResult parent,
		List<CategoryResult> children
	) {
		CategoryGroup group = category.groupCode();
		return new CategoryResult(
			category.id(),
			category.domain(),
			category.parentId(),
			category.depth(),
			group,
			group == null ? null : group.label(),
			category.name(),
			category.code(),
			category.fullPath(),
			category.sortOrder(),
			category.status(),
			category.menuVisible(),
			summary.parentIds().contains(category.id()),
			parent,
			children,
			includeCounts ? summary.middleCounts().getOrDefault(category.id(), 0) : null,
			includeCounts ? summary.smallCounts().getOrDefault(category.id(), 0) : null,
			icon,
			category.createdAt(),
			category.updatedAt()
		);
	}

	private CategoryResult simpleResult(
		Category category,
		CategoryTreeSummary summary,
		Map<Long, MediaResult> icons
	) {
		return category == null ? null : toResult(category, summary, false, icons.get(category.id()));
	}

	private List<CategoryResult> childResults(
		Category category,
		Map<Long, List<Category>> childrenByParentId,
		CategoryTreeSummary summary,
		Map<Long, MediaResult> icons
	) {
		return childrenByParentId.getOrDefault(category.id(), List.of()).stream()
			.map(child -> toResult(
				child,
				summary,
				false,
				icons.get(child.id()),
				null,
				child.depth() < MAX_DEPTH ? childResults(child, childrenByParentId, summary, icons) : null
			))
			.toList();
	}

	private Map<Long, List<Category>> childrenByParentId(List<Category> categories) {
		return categories.stream()
			.filter(category -> category.parentId() != null)
			.collect(Collectors.groupingBy(
				Category::parentId,
				LinkedHashMap::new,
				Collectors.toList()
			));
	}

	private CategoryTreeSummary summarize(CategoryDomain domain) {
		return summarize(categoryRepository.findAllByDomain(domain, Sort.by("id")));
	}

	private CategoryTreeSummary summarize(List<Category> categories) {
		Set<Long> parentIds = new HashSet<>();
		Map<Long, Integer> middleCounts = new HashMap<>();
		Map<Long, Integer> smallCounts = new HashMap<>();
		for (Category child : categories) {
			if (child.parentId() != null) {
				parentIds.add(child.parentId());
				if (child.depth() == 2) {
					middleCounts.merge(child.parentId(), 1, Integer::sum);
				}
			}
		}
		for (Category parent : categories) {
			if (parent.fullPath() == null) {
				continue;
			}
			int count = (int) categories.stream()
				.filter(category -> category.depth() == 3)
				.filter(category -> category.groupCode() == parent.groupCode())
				.filter(category -> category.fullPath().startsWith(parent.fullPath() + " > "))
				.count();
			smallCounts.put(parent.id(), count);
		}
		return new CategoryTreeSummary(parentIds, middleCounts, smallCounts);
	}

	private Long resolveParentId(SelectCategoriesQuery condition) {
		if (condition.parentId() != null) {
			return categoryRepository.findById(condition.parentId())
				.filter(category -> category.domain() == condition.domain())
				.map(Category::id)
				.orElse(null);
		}
		if (!hasText(condition.parentCode())) {
			return null;
		}
		return categoryRepository.findByDomainAndCode(condition.domain(), condition.parentCode().trim())
			.map(Category::id)
			.orElse(null);
	}

	private Category findParent(CategoryDomain domain, Long parentId) {
		if (parentId == null) {
			return null;
		}
		return categoryRepository.findById(parentId)
			.filter(category -> category.domain() == domain)
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "같은 도메인의 상위 카테고리를 찾을 수 없습니다."));
	}

	private Category findCategory(Long id) {
		return categoryRepository.findById(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
	}

	private Category findLockedCategory(Long id) {
		return categoryRepository.findForUpdateById(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
	}

	private void requireMutableDomain(CategoryDomain domain) {
		if (domain == CategoryDomain.PARTNER) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"업체 카테고리는 JSON 정의 파일에서 관리합니다."
			);
		}
	}

	private void requireDefinitionFieldsMutable(Category category, UpdateCategoryCommand command) {
		if (category.domain() != CategoryDomain.PARTNER) {
			return;
		}
		boolean definitionChangeRequested = command.name() != null
			|| command.codeSpecified()
			|| command.groupCodeSpecified()
			|| command.sortOrder() != null
			|| command.status() != null
			|| command.menuVisible() != null;
		if (definitionChangeRequested) {
			requireMutableDomain(category.domain());
		}
	}

	private MediaResult icon(Long categoryId) {
		return mediaReadService.primary(MediaOwnerType.CATEGORY, categoryId, MediaCollectionPolicy.CATEGORY_ICON);
	}

	private Map<Long, MediaResult> icons(List<Category> categories) {
		Set<Long> ids = categories.stream().map(Category::id).collect(java.util.stream.Collectors.toSet());
		return mediaReadService.primaries(MediaOwnerType.CATEGORY, ids, MediaCollectionPolicy.CATEGORY_ICON);
	}

	private void ensureSiblingNameAvailable(
		CategoryDomain domain,
		Long parentId,
		CategoryGroup groupCode,
		String name,
		Long excludedId
	) {
		boolean exists;
		if (parentId == null) {
			if (groupCode == null) {
				exists = excludedId == null
					? categoryRepository.existsByDomainAndParentIsNullAndGroupCodeIsNullAndName(domain, name)
					: categoryRepository.existsByDomainAndParentIsNullAndGroupCodeIsNullAndNameAndIdNot(domain, name, excludedId);
			} else {
				exists = excludedId == null
					? categoryRepository.existsByDomainAndParentIsNullAndGroupCodeAndName(domain, groupCode, name)
					: categoryRepository.existsByDomainAndParentIsNullAndGroupCodeAndNameAndIdNot(
						domain,
						groupCode,
						name,
						excludedId
					);
			}
		} else {
			exists = excludedId == null
				? categoryRepository.existsByDomainAndParent_IdAndName(domain, parentId, name)
				: categoryRepository.existsByDomainAndParent_IdAndNameAndIdNot(domain, parentId, name, excludedId);
		}
		if (exists) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "같은 상위 카테고리 아래 동일한 이름이 이미 존재합니다.");
		}
	}

	private void ensureCodeAvailable(CategoryDomain domain, String code, Long excludedId) {
		if (code == null) {
			return;
		}
		boolean exists = excludedId == null
			? categoryRepository.existsByDomainAndCode(domain, code)
			: categoryRepository.existsByDomainAndCodeAndIdNot(domain, code, excludedId);
		if (exists) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "같은 도메인에서 이미 사용 중인 카테고리 코드입니다.");
		}
	}

	private CategoryGroup resolveGroup(CategoryDomain domain, Category parent, CategoryGroup requested) {
		if (parent == null) {
			return requested;
		}
		if (requested != null && requested != parent.groupCode()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "하위 카테고리는 상위 카테고리와 같은 그룹만 사용할 수 있습니다.");
		}
		return parent.groupCode();
	}

	private Map<String, String> capture(Category category) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("name", category.name());
		values.put("code", category.code());
		values.put("full_path", category.fullPath());
		values.put("group_code", category.groupCode() == null ? null : category.groupCode().name());
		values.put("sort_order", String.valueOf(category.sortOrder()));
		values.put("status", category.status().name());
		values.put("is_menu_visible", String.valueOf(category.menuVisible()));
		return values;
	}

	private void recordHistory(
		AuthenticatedActor actor,
		Category category,
		String action,
		String reason,
		Map<String, String> before,
		Map<String, String> after
	) {
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_CATEGORY,
			category.id(),
			actor.actorType().name(),
			actor.accountId(),
			action,
			reason,
			null
		);
		Set<String> keys = new HashSet<>();
		keys.addAll(before.keySet());
		keys.addAll(after.keySet());
		for (String key : keys) {
			String beforeValue = before.get(key);
			String afterValue = after.get(key);
			if (!Objects.equals(beforeValue, afterValue)) {
				history.addChange(key, beforeValue, afterValue);
			}
		}
		if (history.changes().isEmpty() && "UPDATED".equals(action)) {
			return;
		}
		operationHistoryRepository.save(history);
	}

	private String required(String value, String message) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, message);
		}
		return normalized;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private String normalizeKeyword(String value) {
		String normalized = trimToNull(value);
		return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
	}

	private boolean contains(String value, String keyword) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private int clamp(int value, int min, int max) {
		return Math.min(Math.max(value, min), max);
	}

	private record CategoryTreeSummary(
		Set<Long> parentIds,
		Map<Long, Integer> middleCounts,
		Map<Long, Integer> smallCounts
	) {
	}
}
