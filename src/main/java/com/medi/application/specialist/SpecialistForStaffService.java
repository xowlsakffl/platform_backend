package com.medi.application.specialist;

import com.medi.application.auth.PermissionService;
import com.medi.application.specialist.command.UpdateSpecialistStatusForStaffCommand;
import com.medi.application.specialist.command.SaveSpecialistCommand;
import com.medi.application.specialist.command.UpdateSpecialistForStaffCommand;
import com.medi.application.specialist.query.SearchSpecialistsForStaffQuery;
import com.medi.application.specialist.result.SpecialistDeletedResult;
import com.medi.application.specialist.result.SpecialistDetailResult;
import com.medi.application.specialist.result.PartnerOptionForStaffResult;
import com.medi.application.specialist.result.SpecialistListItemResult;
import com.medi.application.specialist.result.SpecialistListCategoryResult;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.security.AccessPermissions;
import com.medi.common.web.PaginatedResponse;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.category.Category;
import com.medi.domain.category.CategoryAssignmentTarget;
import com.medi.domain.category.CategoryDomain;
import com.medi.domain.specialist.Specialist;
import com.medi.domain.specialist.SpecialistAllowStatus;
import com.medi.domain.specialist.SpecialistField;
import com.medi.domain.partner.Partner;
import com.medi.infrastructure.persistence.category.CategoryRepository;
import com.medi.infrastructure.persistence.specialist.SpecialistRepository;
import com.medi.infrastructure.persistence.partner.PartnerRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SpecialistForStaffService {

	private final PermissionService permissionService;
	private final SpecialistRepository specialistRepository;
	private final PartnerRepository partnerRepository;
	private final CategoryRepository categoryRepository;
	private final SpecialistWriteService specialistWriteService;
	private final SpecialistResultAssembler resultAssembler;
	private final SpecialistHistoryService historyService;
	private final SpecialistLifecycleService lifecycleService;

	public SpecialistForStaffService(
		PermissionService permissionService,
		SpecialistRepository specialistRepository,
		PartnerRepository partnerRepository,
		CategoryRepository categoryRepository,
		SpecialistWriteService specialistWriteService,
		SpecialistResultAssembler resultAssembler,
		SpecialistHistoryService historyService,
		SpecialistLifecycleService lifecycleService
	) {
		this.permissionService = permissionService;
		this.specialistRepository = specialistRepository;
		this.partnerRepository = partnerRepository;
		this.categoryRepository = categoryRepository;
		this.specialistWriteService = specialistWriteService;
		this.resultAssembler = resultAssembler;
		this.historyService = historyService;
		this.lifecycleService = lifecycleService;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<SpecialistListItemResult> list(AuthenticatedActor actor, SearchSpecialistsForStaffQuery condition) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_SHOW);
		validateMetricRange(condition);
		Set<Long> expandedCategoryIds = expandCategoryIds(condition.categoryIds());
		Page<Specialist> page = specialistRepository.findAll(
			specification(condition, expandedCategoryIds),
			PageRequest.of(
				Math.max(condition.page(), 1) - 1,
				Math.min(Math.max(condition.perPage(), 1), 100),
				sort(condition)
			)
		);
		List<Specialist> specialists = page.getContent();
		Map<Long, List<SpecialistListCategoryResult>> categories = resultAssembler.listCategoriesBySpecialistIds(specialists);
		Map<Long, MediaResult> profileImages = resultAssembler.profileImages(specialists);

		return PaginatedResponse.from(page, specialist -> resultAssembler.listItem(
			specialist,
			categories.getOrDefault(specialist.id(), List.of()),
			profileImages.get(specialist.id()),
			SpecialistMediaAccessScope.STAFF
		));
	}

	@Transactional(readOnly = true)
	public SpecialistDetailResult get(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_SHOW);
		return resultAssembler.detail(findActiveSpecialist(id), SpecialistMediaAccessScope.STAFF);
	}

	@Transactional(readOnly = true)
	public List<PartnerOptionForStaffResult> partnerOptions(AuthenticatedActor actor, String q, int limit) {
		permissionService.requireStaffPermission(actor, AccessPermissions.PARTNER_SHOW);
		String keyword = trimToNull(q);
		Specification<Partner> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.isNull(root.get("deletedAt")));
			if (keyword != null) {
				predicates.add(builder.like(root.get("name"), escapeLike(keyword) + "%", '\\'));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
		return partnerRepository.findAll(
			specification,
			PageRequest.of(
				0,
				Math.min(Math.max(limit, 1), 20),
				Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
			)
		).stream().map(partner -> new PartnerOptionForStaffResult(
			partner.id(),
			partner.name(),
			partner.businessRegistration() == null ? null : partner.businessRegistration().businessNumber()
		)).toList();
	}

	@Transactional
	public SpecialistDetailResult create(AuthenticatedActor actor, SaveSpecialistCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_CREATE);
		Partner partner = findLockedPartner(command.partnerId());
		Specialist saved = specialistWriteService.create(partner, command);
		historyService.record(actor, saved, "CREATED", null, Map.of(), historyService.capture(saved));

		return resultAssembler.detail(saved, SpecialistMediaAccessScope.STAFF);
	}

	@Transactional
	public SpecialistDetailResult update(AuthenticatedActor actor, Long id, UpdateSpecialistForStaffCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_UPDATE);
		Specialist reference = findActiveSpecialist(id);
		Long targetPartnerId = command.specified("partner_id") ? command.partnerId() : reference.partnerId();
		if (targetPartnerId == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "파트너은 필수입니다.");
		}
		if (command.specified("allow_status")
			&& command.allowStatus() == SpecialistAllowStatus.REJECTED
			&& trimToNull(command.reason()) == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "반려 사유를 입력해주세요.");
		}
		Map<Long, Partner> lockedPartners = lockPartners(reference.partnerId(), targetPartnerId);
		Specialist specialist = findLockedSpecialist(id);
		Partner partner = lockedPartners.get(targetPartnerId);
		if (partner == null) {
			throw new ApiException(ErrorCode.NOT_FOUND, "파트너을 찾을 수 없습니다.");
		}
		Map<String, String> before = historyService.capture(specialist);
		Specialist saved = specialistWriteService.updatePartial(specialist, partner, command);
		String reason = command.specified("allow_status")
			&& !Objects.equals(before.get("allow_status"), saved.allowStatus().name())
			? trimToNull(command.reason())
			: null;
		historyService.record(actor, saved, "UPDATED", reason, before, historyService.capture(saved));
		return resultAssembler.detail(saved, SpecialistMediaAccessScope.STAFF);
	}

	@Transactional
	public SpecialistDetailResult patch(AuthenticatedActor actor, Long id, UpdateSpecialistStatusForStaffCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_UPDATE);
		if (command.status() == null && command.allowStatus() == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "변경할 스페셜리스트 상태가 없습니다.");
		}
		Specialist reference = findActiveSpecialist(id);
		findLockedPartner(reference.partnerId());
		Specialist specialist = findLockedSpecialist(id);
		Map<String, String> before = historyService.capture(specialist);
		if (command.status() != null) {
			specialist.changeStatus(command.status());
		}
		if (command.allowStatus() != null) {
			if (command.allowStatus() == SpecialistAllowStatus.REJECTED && trimToNull(command.reason()) == null) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "반려 사유를 입력해주세요.");
			}
			specialist.changeAllowStatus(command.allowStatus());
		}
		historyService.record(
			actor,
			specialist,
			"STATE_UPDATED",
			trimToNull(command.reason()),
			before,
			historyService.capture(specialist)
		);
		return resultAssembler.detail(specialistRepository.saveAndFlush(specialist), SpecialistMediaAccessScope.STAFF);
	}

	@Transactional
	public SpecialistDeletedResult delete(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_DELETE);
		Specialist reference = findActiveSpecialist(id);
		findLockedPartner(reference.partnerId());
		Specialist specialist = findLockedSpecialist(id);
		Map<String, String> before = historyService.capture(specialist);
		lifecycleService.softDelete(specialist);
		historyService.record(actor, specialist, "DELETED", null, before, Map.of());
		specialistRepository.saveAndFlush(specialist);
		return new SpecialistDeletedResult(specialist.id(), specialist.deletedAt());
	}

	private Specification<Specialist> specification(SearchSpecialistsForStaffQuery condition, Set<Long> expandedCategoryIds) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.isNull(root.get("deletedAt")));
			predicates.add(builder.isNull(root.get("partner").get("deletedAt")));
			if (condition.partnerId() != null) {
				predicates.add(builder.equal(root.get("partner").get("id"), condition.partnerId()));
			}
			String keyword = trimToNull(condition.q());
			if (keyword != null) {
				List<Predicate> matches = new ArrayList<>();
				matches.add(builder.like(root.get("name"), "%" + keyword + "%"));
				matches.add(builder.like(root.get("partner").get("name"), "%" + keyword + "%"));
				String digits = normalizeLicenseNumber(keyword);
				if (!digits.isEmpty()) {
					matches.add(builder.like(root.get("licenseNumber"), "%" + digits + "%"));
				}
				parseLong(keyword).ifPresent(id -> matches.add(builder.equal(root.get("id"), id)));
				predicates.add(builder.or(matches.toArray(Predicate[]::new)));
			}
			if (!condition.allowStatus().isEmpty()) {
				predicates.add(root.get("allowStatus").in(condition.allowStatus()));
			}
			if (!condition.positions().isEmpty()) {
				predicates.add(root.get("position").in(condition.positions()));
			}
			if (!condition.specialistFields().isEmpty()) {
				predicates.add(root.get("specialistField").in(condition.specialistFields()));
			}
			if (!condition.categoryIds().isEmpty()) {
				if (expandedCategoryIds.isEmpty()) {
					predicates.add(builder.disjunction());
					return builder.and(predicates.toArray(Predicate[]::new));
				}
				Subquery<Long> subquery = query.subquery(Long.class);
				var assignment = subquery.from(CategoryAssignment.class);
				subquery.select(assignment.get("categorizableId")).where(
					builder.equal(assignment.get("categorizableType"), CategoryAssignment.SPECIALIST_TARGET_TYPE),
					assignment.get("category").get("id").in(expandedCategoryIds)
				);
				predicates.add(root.get("id").in(subquery));
			}
			applyMetric(predicates, builder, root.get("careerStartedAt"), condition);
			applyDateRange(predicates, builder, root.get("createdAt"), condition.startDate(), condition.endDate());
			if (condition.sort() == null && !Long.class.equals(query.getResultType())) {
				query.orderBy(
					builder.asc(builder.selectCase()
						.when(builder.equal(root.get("allowStatus"), SpecialistAllowStatus.PENDING), 0)
						.otherwise(1)),
					builder.desc(root.get("createdAt")),
					builder.desc(root.get("id"))
				);
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Sort sort(SearchSpecialistsForStaffQuery condition) {
		if (condition.sort() == null) {
			return Sort.unsorted();
		}
		Sort.Direction direction = "asc".equalsIgnoreCase(condition.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
		String field = condition.sort();
		if ("review_count".equals(field) || "consultation_count".equals(field)) {
			return Sort.by(Sort.Order.desc("id"));
		}
		if ("id".equals(field)) {
			return Sort.by(new Sort.Order(direction, "id"));
		}
		if ("career_years".equals(field)) {
			Sort.Direction careerDirection = direction == Sort.Direction.ASC ? Sort.Direction.DESC : Sort.Direction.ASC;
			return Sort.by(new Sort.Order(careerDirection, "careerStartedAt"), Sort.Order.desc("id"));
		}
		String property = switch (field) {
			case "name" -> "name";
			case "gender" -> "gender";
			case "position" -> "position";
			case "specialist_field" -> "specialistField";
			case "allow_status" -> "allowStatus";
			case "created_at" -> "createdAt";
			default -> "id";
		};
		return Sort.by(new Sort.Order(direction, property), Sort.Order.desc("id"));
	}

	private void applyMetric(
		List<Predicate> predicates,
		jakarta.persistence.criteria.CriteriaBuilder builder,
		jakarta.persistence.criteria.Path<LocalDate> careerStartedAt,
		SearchSpecialistsForStaffQuery condition
	) {
		if (condition.metric() == null) {
			return;
		}
		if ("career_years".equals(condition.metric())) {
			predicates.add(builder.isNotNull(careerStartedAt));
			LocalDate today = LocalDate.now();
			if (condition.metricMin() != null) {
				predicates.add(builder.lessThanOrEqualTo(careerStartedAt, today.minusYears(condition.metricMin())));
			}
			if (condition.metricMax() != null) {
				predicates.add(builder.greaterThan(careerStartedAt, today.minusYears((long) condition.metricMax() + 1)));
			}
			return;
		}
		if (condition.metricMin() != null && condition.metricMin() > 0) {
			predicates.add(builder.disjunction());
		}
	}

	private void applyDateRange(
		List<Predicate> predicates,
		jakarta.persistence.criteria.CriteriaBuilder builder,
		jakarta.persistence.criteria.Path<LocalDateTime> path,
		String startDate,
		String endDate
	) {
		if (startDate != null) {
			predicates.add(builder.greaterThanOrEqualTo(path, parseDate(startDate).atStartOfDay()));
		}
		if (endDate != null) {
			predicates.add(builder.lessThan(path, parseDate(endDate).plusDays(1).atStartOfDay()));
		}
	}

	private Specialist findActiveSpecialist(Long id) {
		return specialistRepository.findByIdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "스페셜리스트을 찾을 수 없습니다."));
	}

	private Specialist findLockedSpecialist(Long id) {
		return specialistRepository.findForUpdateByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "스페셜리스트을 찾을 수 없습니다."));
	}

	private Partner findLockedPartner(Long id) {
		return partnerRepository.findForUpdateByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "파트너을 찾을 수 없습니다."));
	}

	private Map<Long, Partner> lockPartners(Long firstId, Long secondId) {
		Set<Long> ids = new HashSet<>();
		ids.add(firstId);
		ids.add(secondId);
		Map<Long, Partner> result = new HashMap<>();
		ids.stream().sorted().forEach(id -> result.put(id, findLockedPartner(id)));
		return result;
	}

	private String normalizeLicenseNumber(String value) {
		return value == null ? "" : value.replaceAll("\\D", "");
	}

	private Set<Long> expandCategoryIds(Set<Long> selectedIds) {
		if (selectedIds.isEmpty()) {
			return Set.of();
		}
		List<Category> selected = categoryRepository.findByIdIn(selectedIds);
		boolean invalid = selected.size() != selectedIds.size()
			|| selected.stream().anyMatch(category -> !CategoryAssignmentTarget.SPECIALIST.accepts(category));
		if (invalid) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 시술분야 카테고리가 올바르지 않습니다.");
		}

		Set<Long> expanded = new LinkedHashSet<>(selectedIds);
		for (Category category : selected) {
			String path = StringUtils.hasText(category.fullPath()) ? category.fullPath() : category.name();
			expanded.addAll(categoryRepository
				.findByDomainAndGroupCodeAndFullPathStartingWithOrderByDepthAsc(
					CategoryDomain.BEAUTY,
					category.groupCode(),
					path + " > "
				)
				.stream()
				.map(Category::id)
				.toList());
		}
		return Set.copyOf(expanded);
	}

	private String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private void validateMetricRange(SearchSpecialistsForStaffQuery condition) {
		if (condition.metricMin() != null && condition.metricMax() != null && condition.metricMin() > condition.metricMax()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "지표 최소값은 최대값보다 클 수 없습니다.");
		}
	}

	private LocalDate parseDate(String value) {
		try {
			return LocalDate.parse(value);
		} catch (RuntimeException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "날짜 형식은 YYYY-MM-DD여야 합니다.");
		}
	}

	private java.util.Optional<Long> parseLong(String value) {
		try {
			return java.util.Optional.of(Long.parseLong(value));
		} catch (NumberFormatException exception) {
			return java.util.Optional.empty();
		}
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
