package com.medi.application.hospital;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.auth.PermissionService;
import com.medi.application.cache.StaffSummaryCache;
import com.medi.application.cache.StaffSummaryCacheInvalidator;
import com.medi.application.doctor.DoctorLifecycleService;
import com.medi.application.doctor.result.DoctorSpecialistResult;
import com.medi.application.hospital.command.CreateHospitalCommand;
import com.medi.application.hospital.command.ChangeHospitalAllowStatusCommand;
import com.medi.application.hospital.command.ChangeHospitalStatusCommand;
import com.medi.application.hospital.command.HospitalBusinessRegistrationCommand;
import com.medi.application.hospital.command.HospitalContactSetCommand;
import com.medi.application.hospital.command.UpdateHospitalCommand;
import com.medi.application.hospital.query.SearchHospitalsQuery;
import com.medi.application.hospital.query.GetHospitalForStaffQuery;
import com.medi.application.hospital.query.SearchHospitalOperationHistoriesForStaffQuery;
import com.medi.application.hospital.result.DuplicateCheckResult;
import com.medi.application.hospital.result.HospitalAccountResult;
import com.medi.application.hospital.result.HospitalAllowStatusBulkUpdateResult;
import com.medi.application.hospital.result.HospitalBusinessRegistrationResult;
import com.medi.application.hospital.result.HospitalCategoryResult;
import com.medi.application.hospital.result.HospitalContactGroupResult;
import com.medi.application.hospital.result.HospitalContactResult;
import com.medi.application.hospital.result.HospitalDeletedResult;
import com.medi.application.hospital.result.HospitalDetailResult;
import com.medi.application.hospital.result.HospitalDoctorForStaffResult;
import com.medi.application.hospital.result.HospitalEvaluationResult;
import com.medi.application.hospital.result.HospitalFeatureResult;
import com.medi.application.hospital.result.HospitalInterpretationLanguageResult;
import com.medi.application.hospital.result.HospitalListItemResult;
import com.medi.application.hospital.result.HospitalReviewCountsResult;
import com.medi.application.hospital.result.HospitalSummaryResult;
import com.medi.application.hospital.result.HospitalSettlementAccountResult;
import com.medi.application.hospital.result.OperationHistoryChangeResult;
import com.medi.application.hospital.result.OperationHistoryResult;
import com.medi.application.media.MediaLifecycleService;
import com.medi.application.media.MediaCollectionPolicy;
import com.medi.application.media.MediaCommandService;
import com.medi.application.media.MediaReadService;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.error.InternalApplicationException;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.security.AccessPermissions;
import com.medi.common.web.PaginatedResponse;
import com.medi.domain.account.AccountHospital;
import com.medi.domain.account.AccountHospitalStatus;
import com.medi.domain.category.Category;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.category.CategoryAssignmentTarget;
import com.medi.domain.category.CategoryDomain;
import com.medi.domain.category.CategoryStatus;
import com.medi.domain.hospital.Hospital;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalBusinessRegistration;
import com.medi.domain.hospital.HospitalContact;
import com.medi.domain.hospital.HospitalContactType;
import com.medi.domain.hospital.HospitalFeature;
import com.medi.domain.hospital.HospitalFeatureStatus;
import com.medi.domain.hospital.HospitalInterpretationLanguage;
import com.medi.domain.hospital.HospitalStatus;
import com.medi.domain.doctor.Doctor;
import com.medi.domain.media.MediaOwnerType;
import com.medi.domain.operationhistory.OperationHistory;
import com.medi.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.medi.infrastructure.persistence.category.CategoryRepository;
import com.medi.infrastructure.persistence.account.AccountHospitalRepository;
import com.medi.infrastructure.persistence.hospital.HospitalBusinessRegistrationRepository;
import com.medi.infrastructure.persistence.hospital.HospitalFeatureRepository;
import com.medi.infrastructure.persistence.hospital.HospitalRepository;
import com.medi.infrastructure.persistence.doctor.DoctorRepository;
import com.medi.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HospitalForStaffService {

	private static final String ACTION_CREATED = "ACTION_CREATED";
	private static final String ACTION_UPDATED = "ACTION_UPDATED";
	private static final String ACTION_STATUS_UPDATED = "ACTION_STATUS_UPDATED";
	private static final String ACTION_ALLOW_STATUS_UPDATED = "ACTION_ALLOW_STATUS_UPDATED";
	private static final String ACTION_DELETED = "ACTION_DELETED";
	private final PermissionService permissionService;
	private final HospitalRepository hospitalRepository;
	private final AccountHospitalRepository accountHospitalRepository;
	private final HospitalBusinessRegistrationRepository businessRegistrationRepository;
	private final HospitalFeatureRepository featureRepository;
	private final CategoryRepository categoryRepository;
	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final OperationHistoryRepository operationHistoryRepository;
	private final MediaLifecycleService mediaLifecycleService;
	private final MediaCommandService mediaCommandService;
	private final MediaReadService mediaReadService;
	private final DoctorLifecycleService doctorLifecycleService;
	private final DoctorRepository doctorRepository;
	private final StaffSummaryCache staffSummaryCache;
	private final StaffSummaryCacheInvalidator summaryCacheInvalidator;
	private final ObjectMapper objectMapper;

	public HospitalForStaffService(
		PermissionService permissionService,
		HospitalRepository hospitalRepository,
		AccountHospitalRepository accountHospitalRepository,
		HospitalBusinessRegistrationRepository businessRegistrationRepository,
		HospitalFeatureRepository featureRepository,
		CategoryRepository categoryRepository,
		CategoryAssignmentRepository categoryAssignmentRepository,
		OperationHistoryRepository operationHistoryRepository,
		MediaLifecycleService mediaLifecycleService,
		MediaCommandService mediaCommandService,
		MediaReadService mediaReadService,
		DoctorLifecycleService doctorLifecycleService,
		DoctorRepository doctorRepository,
		StaffSummaryCache staffSummaryCache,
		StaffSummaryCacheInvalidator summaryCacheInvalidator,
		ObjectMapper objectMapper
	) {
		this.permissionService = permissionService;
		this.hospitalRepository = hospitalRepository;
		this.accountHospitalRepository = accountHospitalRepository;
		this.businessRegistrationRepository = businessRegistrationRepository;
		this.featureRepository = featureRepository;
		this.categoryRepository = categoryRepository;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
		this.operationHistoryRepository = operationHistoryRepository;
		this.mediaLifecycleService = mediaLifecycleService;
		this.mediaCommandService = mediaCommandService;
		this.mediaReadService = mediaReadService;
		this.doctorLifecycleService = doctorLifecycleService;
		this.doctorRepository = doctorRepository;
		this.staffSummaryCache = staffSummaryCache;
		this.summaryCacheInvalidator = summaryCacheInvalidator;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<HospitalListItemResult> list(AuthenticatedActor actor, SearchHospitalsQuery condition) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_SHOW);
		Set<Long> expandedCategoryIds = expandCategoryIds(condition.categoryIds());
		Pageable pageable = PageRequest.of(
			Math.max(condition.page(), 1) - 1,
			clamp(condition.perPage(), 1, 100),
			sort(condition)
		);
		Page<Hospital> page = hospitalRepository.findAll(specification(condition, expandedCategoryIds), pageable);
		boolean includeCategories = includes(condition, "categories");
		boolean includeFeatures = includes(condition, "features");
		Map<Long, List<HospitalCategoryResult>> categories = includeCategories
			? categoriesByHospitalIds(page.getContent())
			: Map.of();
		Map<Long, MediaResult> logos = mediaReadService.primaries(
			MediaOwnerType.HOSPITAL,
			page.getContent().stream().map(Hospital::id).collect(Collectors.toSet()),
			MediaCollectionPolicy.HOSPITAL_LOGO
		);

		return PaginatedResponse.from(page, hospital -> toListItem(
			hospital,
			includeCategories ? categories.getOrDefault(hospital.id(), List.of()) : null,
			includeFeatures ? featureResponses(hospital.features()) : null,
			logos.get(hospital.id())
		));
	}

	@Transactional(readOnly = true)
	public HospitalSummaryResult summary(AuthenticatedActor actor) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_SHOW);
		return staffSummaryCache.remember(StaffSummaryCache.HOSPITAL, HospitalSummaryResult.class, () ->
			new HospitalSummaryResult(
				accountHospitalRepository.countDormantHospitalAccounts(LocalDateTime.now().minusDays(30)),
				hospitalRepository.countByAllowStatus(HospitalAllowStatus.PENDING),
				hospitalRepository.countByAllowStatus(HospitalAllowStatus.REJECTED),
				hospitalRepository.countByDeletedAtIsNullAndStatus(HospitalStatus.SUSPENDED),
				hospitalRepository.countWithdrawnOrDeleted()
			)
		);
	}

	@Transactional(readOnly = true)
	public HospitalDetailResult get(AuthenticatedActor actor, Long id, GetHospitalForStaffQuery query) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_SHOW);
		if (query.includes("doctors")) {
			permissionService.requireStaffPermission(actor, AccessPermissions.DOCTOR_SHOW);
		}
		Hospital hospital = findActiveHospital(id);
		return toDetail(hospital, categories(hospital.id()), query.include());
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<OperationHistoryResult> histories(
		AuthenticatedActor actor,
		Long id,
		SearchHospitalOperationHistoriesForStaffQuery condition
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_SHOW);
		findActiveHospital(id);
		Pageable pageable = PageRequest.of(
			Math.max(condition.page(), 1) - 1,
			clamp(condition.perPage(), 1, 50),
			Sort.by(Sort.Direction.DESC, "createdAt", "id")
		);
		Page<OperationHistory> page = operationHistoryRepository.findByTargetTypeAndTargetId(
			OperationHistory.TARGET_HOSPITAL,
			id,
			pageable
		);
		return PaginatedResponse.from(page, this::operationHistoryResult);
	}

	@Transactional
	public HospitalDetailResult create(AuthenticatedActor actor, CreateHospitalCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_CREATE);
		if (hospitalRepository.existsByNameAndDeletedAtIsNull(command.name())) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 병원명입니다.");
		}
		HospitalBusinessRegistrationCommand businessCommand = requireBusinessRegistration(command.businessRegistration());
		String businessNumber = normalizeBusinessNumber(businessCommand.businessNumber());
		if (businessRegistrationRepository.existsByBusinessNumber(businessNumber)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 사업자등록번호입니다.");
		}

		Hospital hospital = new Hospital(
			trim(command.name()),
			trimToNull(command.description()),
			normalizeYoutubeLink(command.youtubeLink()),
			trimToNull(command.address()),
			trimToNull(command.addressDetail()),
			trimToNull(command.latitude()),
			trimToNull(command.longitude()),
			trimToNull(command.consultingHours()),
			normalizeOperationHours(command.operationHours()),
			trimToNull(command.direction()),
			command.allowStatus(),
			command.status()
		);
		hospital.replaceContacts(buildContacts(requireContacts(command.contacts()), true));
		hospital.replaceBusinessRegistration(toBusinessRegistration(businessCommand, businessNumber));
		hospital.replaceFeatures(loadFeatures(command.featureIds()));
		hospital.replaceInterpretationLanguages(
			command.interpretationLanguages() == null ? Set.of() : command.interpretationLanguages()
		);

		Hospital saved = hospitalRepository.saveAndFlush(hospital);
		syncCategories(saved.id(), command.categoryIds());
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.HOSPITAL,
			saved.id(),
			MediaCollectionPolicy.HOSPITAL_LOGO,
			command.logo(),
			null,
			true
		);
		mediaCommandService.synchronizeMany(
			MediaOwnerType.HOSPITAL,
			saved.id(),
			MediaCollectionPolicy.HOSPITAL_GALLERY,
			command.gallery(),
			List.of(),
			true,
			5
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.HOSPITAL_BUSINESS_REGISTRATION,
			saved.businessRegistration().id(),
			MediaCollectionPolicy.HOSPITAL_BUSINESS_REGISTRATION_FILE,
			command.businessRegistrationFile(),
			null,
			true
		);
		recordSimpleHistory(actor, saved, ACTION_CREATED, null);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.HOSPITAL);

		return toDetail(findActiveHospital(saved.id()), categories(saved.id()));
	}

	@Transactional
	public HospitalDetailResult update(AuthenticatedActor actor, Long id, UpdateHospitalCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_UPDATE);
		Hospital hospital = findActiveHospital(id);
		Map<String, String> before = capture(hospital);

		if (command.contacts() != null) {
			hospital.replaceContacts(buildContacts(
				mergeContacts(hospital, command.contacts(), command.specifiedFields()),
				true
			));
		}
		if (command.businessRegistration() != null) {
			HospitalBusinessRegistrationCommand businessCommand = command.businessRegistration();
			String businessNumber = command.specified("business_number")
				? normalizeBusinessNumber(businessCommand.businessNumber())
				: hospital.businessRegistration().businessNumber();
			assertBusinessNumberAvailableForUpdate(hospital, businessNumber);
			applyBusinessRegistration(hospital, businessCommand, businessNumber, command.specifiedFields());
		}
		if (command.featureIds() != null) {
			hospital.replaceFeatures(loadFeatures(command.featureIds()));
		}
		if (command.interpretationLanguages() != null) {
			hospital.replaceInterpretationLanguages(command.interpretationLanguages());
		}

		hospital.updateProfile(
			command.specified("description") ? trimToNull(command.description()) : hospital.description(),
			command.specified("youtube_link") ? normalizeYoutubeLink(command.youtubeLink()) : hospital.youtubeLink(),
			command.specified("address") ? trimToNull(command.address()) : hospital.address(),
			command.specified("address_detail") ? trimToNull(command.addressDetail()) : hospital.addressDetail(),
			command.specified("latitude") ? trimToNull(command.latitude()) : hospital.latitude(),
			command.specified("longitude") ? trimToNull(command.longitude()) : hospital.longitude(),
			command.specified("consulting_hours") ? trimToNull(command.consultingHours()) : hospital.consultingHours(),
			command.specified("operation_hours")
				? normalizeOperationHours(command.operationHours())
				: hospital.operationHours(),
			command.specified("direction") ? trimToNull(command.direction()) : hospital.direction(),
			command.allowStatus(),
			command.status()
		);
		Hospital saved = hospitalRepository.saveAndFlush(hospital);

		if (command.categoryIds() != null) {
			syncCategories(saved.id(), command.categoryIds());
		}
		if (command.specified("logo") || command.specified("existing_logo_id")) {
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.HOSPITAL,
				saved.id(),
				MediaCollectionPolicy.HOSPITAL_LOGO,
				command.logo(),
				command.existingLogoId(),
				false
			);
		}
		if (command.specified("gallery_order")) {
			mediaCommandService.synchronizeManyOrdered(
				MediaOwnerType.HOSPITAL,
				saved.id(),
				MediaCollectionPolicy.HOSPITAL_GALLERY,
				command.gallery(),
				command.galleryOrder(),
				false,
				5
			);
		} else if (command.specified("gallery") || command.specified("existing_gallery_ids")) {
			mediaCommandService.synchronizeMany(
				MediaOwnerType.HOSPITAL,
				saved.id(),
				MediaCollectionPolicy.HOSPITAL_GALLERY,
				command.gallery(),
				command.existingGalleryIds(),
				false,
				5
			);
		}
		if (command.specified("business_registration_file")
			|| command.specified("existing_business_registration_file_id")) {
			mediaCommandService.synchronizeSingle(
				MediaOwnerType.HOSPITAL_BUSINESS_REGISTRATION,
				saved.businessRegistration().id(),
				MediaCollectionPolicy.HOSPITAL_BUSINESS_REGISTRATION_FILE,
				command.businessRegistrationFile(),
				command.existingBusinessRegistrationFileId(),
				false
			);
		}
		recordChangedHistory(actor, saved, ACTION_UPDATED, null, before, capture(saved));
		if (command.specified("allow_status") || command.specified("status")) {
			summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.HOSPITAL);
		}

		return toDetail(findActiveHospital(saved.id()), categories(saved.id()));
	}

	@Transactional
	public HospitalDetailResult changeStatus(AuthenticatedActor actor, Long id, ChangeHospitalStatusCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_UPDATE);
		Hospital hospital = findActiveHospital(id);
		HospitalStatus before = hospital.status();
		hospital.changeStatus(command.status());
		Hospital saved = hospitalRepository.saveAndFlush(hospital);

		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_HOSPITAL,
			saved.id(),
			actor.actorType().name(),
			actor.accountId(),
			ACTION_STATUS_UPDATED,
			trimToNull(command.reason()),
			null
		);
		history.addChange("status", before.name(), command.status().name());
		operationHistoryRepository.save(history);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.HOSPITAL);

		return toDetail(findActiveHospital(saved.id()), categories(saved.id()));
	}

	@Transactional
	public HospitalAllowStatusBulkUpdateResult changeAllowStatus(
		AuthenticatedActor actor,
		ChangeHospitalAllowStatusCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_UPDATE);
		List<Long> normalizedIds = command.ids().stream()
			.filter(Objects::nonNull)
			.filter(id -> id > 0)
			.distinct()
			.toList();
		if (normalizedIds.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "변경할 병원을 선택해주세요.");
		}
		List<Hospital> hospitals = hospitalRepository.findByIdInAndDeletedAtIsNull(normalizedIds);
		if (hospitals.isEmpty()) {
			throw new ApiException(ErrorCode.NOT_FOUND, "변경할 병원을 찾을 수 없습니다.");
		}

		int updatedCount = 0;
		for (Hospital hospital : hospitals) {
			HospitalAllowStatus before = hospital.allowStatus();
			if (before == command.allowStatus()) {
				continue;
			}
			hospital.changeAllowStatus(command.allowStatus());
			OperationHistory history = new OperationHistory(
				OperationHistory.TARGET_HOSPITAL,
				hospital.id(),
				actor.actorType().name(),
				actor.accountId(),
				ACTION_ALLOW_STATUS_UPDATED,
				trimToNull(command.reason()),
				null
			);
			history.addChange("allow_status", before.name(), command.allowStatus().name());
			operationHistoryRepository.save(history);
			updatedCount++;
		}
		hospitalRepository.saveAll(hospitals);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.HOSPITAL);

		return new HospitalAllowStatusBulkUpdateResult(
			updatedCount,
			command.allowStatus().name(),
			hospitals.stream().map(Hospital::id).toList()
		);
	}

	@Transactional
	public HospitalDeletedResult delete(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_DELETE);
		Hospital hospital = findLockedActiveHospital(id);
		doctorLifecycleService.softDeleteByHospital(hospital.id());
		if (hospital.businessRegistration() != null) {
			mediaLifecycleService.softDeleteOwnedMedia(
				MediaOwnerType.HOSPITAL_BUSINESS_REGISTRATION,
				hospital.businessRegistration().id()
			);
		}
		categoryAssignmentRepository.deleteByCategorizableTypeAndCategorizableId(
			CategoryAssignment.HOSPITAL_TARGET_TYPE,
			hospital.id()
		);
		hospital.softDelete();
		mediaLifecycleService.softDeleteOwnedMedia(MediaOwnerType.HOSPITAL, hospital.id());
		Hospital saved = hospitalRepository.saveAndFlush(hospital);
		recordSimpleHistory(actor, saved, ACTION_DELETED, null);
		summaryCacheInvalidator.forgetAfterCommit(StaffSummaryCache.HOSPITAL);
		return new HospitalDeletedResult(saved.id(), saved.deletedAt());
	}

	@Transactional(readOnly = true)
	public DuplicateCheckResult checkName(AuthenticatedActor actor, String name) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_CREATE);
		return new DuplicateCheckResult(hospitalRepository.existsByNameAndDeletedAtIsNull(trim(name)));
	}

	@Transactional(readOnly = true)
	public DuplicateCheckResult checkBusinessNumber(AuthenticatedActor actor, String businessNumber) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_CREATE);
		return new DuplicateCheckResult(
			businessRegistrationRepository.existsByBusinessNumber(normalizeBusinessNumber(businessNumber))
		);
	}

	private Specification<Hospital> specification(SearchHospitalsQuery condition, Set<Long> expandedCategoryIds) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

			String q = trimToNull(condition.q());
			if (q != null) {
				var accountJoin = root.join("accountHospital", JoinType.LEFT);
				List<Predicate> searchPredicates = new ArrayList<>();
				searchPredicates.add(criteriaBuilder.like(root.get("name"), "%" + q + "%"));
				searchPredicates.add(criteriaBuilder.like(accountJoin.get("nickname"), "%" + q + "%"));
				parseLong(q).ifPresent(value -> searchPredicates.add(criteriaBuilder.equal(root.get("id"), value)));
				predicates.add(criteriaBuilder.or(searchPredicates.toArray(Predicate[]::new)));
			}
			if (condition.status() != null && !condition.status().isEmpty()) {
				predicates.add(root.get("status").in(condition.status()));
			}
			if (condition.accountStatus() != null && !condition.accountStatus().isEmpty()) {
				var accountJoin = root.join("accountHospital", JoinType.INNER);
				predicates.add(accountJoin.get("status").in(condition.accountStatus()));
			}
			if (condition.allowStatus() != null && !condition.allowStatus().isEmpty()) {
				predicates.add(root.get("allowStatus").in(condition.allowStatus()));
			}
			if (Boolean.TRUE.equals(condition.dormant())) {
				var accountJoin = root.join("accountHospital", JoinType.INNER);
				predicates.add(criteriaBuilder.notEqual(root.get("status"), HospitalStatus.WITHDRAWN));
				predicates.add(criteriaBuilder.isNull(accountJoin.get("deletedAt")));
				predicates.add(criteriaBuilder.notEqual(accountJoin.get("status"), AccountHospitalStatus.WITHDRAWN));
				predicates.add(criteriaBuilder.or(
					criteriaBuilder.isNull(accountJoin.get("lastLoginAt")),
					criteriaBuilder.lessThan(accountJoin.<LocalDateTime>get("lastLoginAt"), LocalDateTime.now().minusDays(30))
				));
			}
			applyDateRange(predicates, criteriaBuilder, root.<LocalDateTime>get("createdAt"), condition.startDate(), condition.endDate());
			applyDateRange(predicates, criteriaBuilder, root.<LocalDateTime>get("updatedAt"), condition.updatedStartDate(), condition.updatedEndDate());
			if (expandedCategoryIds != null && !expandedCategoryIds.isEmpty()) {
				Subquery<Long> subquery = query.subquery(Long.class);
				var assignment = subquery.from(CategoryAssignment.class);
				subquery.select(assignment.get("categorizableId"))
					.where(
						criteriaBuilder.equal(assignment.get("categorizableType"), CategoryAssignment.HOSPITAL_TARGET_TYPE),
						assignment.get("category").get("id").in(expandedCategoryIds)
					);
				predicates.add(root.get("id").in(subquery));
			}
			if (!Long.class.equals(query.getResultType())
				&& !long.class.equals(query.getResultType())
				&& "last_login_at".equals(condition.sort())) {
				var accountJoin = root.join("accountHospital", JoinType.LEFT);
				query.orderBy("asc".equalsIgnoreCase(condition.direction())
					? criteriaBuilder.asc(accountJoin.get("lastLoginAt"))
					: criteriaBuilder.desc(accountJoin.get("lastLoginAt")));
			}
			if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType()) && !StringUtils.hasText(condition.sort())) {
				query.orderBy(
					criteriaBuilder.asc(
						criteriaBuilder.<Integer>selectCase()
							.when(criteriaBuilder.equal(root.get("allowStatus"), HospitalAllowStatus.PENDING), 0)
							.otherwise(1)
					),
					criteriaBuilder.desc(root.get("createdAt")),
					criteriaBuilder.desc(root.get("id"))
				);
			}
			query.distinct(true);
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Sort sort(SearchHospitalsQuery condition) {
		String sort = trimToNull(condition.sort());
		if (sort == null) {
			return Sort.unsorted();
		}
		Sort.Direction direction = "asc".equalsIgnoreCase(condition.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
		return switch (sort) {
			case "id" -> Sort.by(direction, "id");
			case "name" -> Sort.by(direction, "name");
			case "updated_at" -> Sort.by(direction, "updatedAt");
			case "created_at" -> Sort.by(direction, "createdAt");
			case "view_count" -> Sort.by(direction, "viewCount");
			case "evaluation_count" -> Sort.by(direction, "evaluationCount");
			case "evaluation_average_rating" -> Sort.by(direction, "evaluationAverageRating");
			case "status" -> Sort.by(direction, "status");
			case "allow_status" -> Sort.by(direction, "allowStatus");
			case "last_login_at" -> Sort.unsorted();
			default -> Sort.unsorted();
		};
	}

	private boolean includes(SearchHospitalsQuery condition, String value) {
		return condition.include() != null && condition.include().contains(value);
	}

	private Set<Long> expandCategoryIds(List<Long> selectedIds) {
		if (selectedIds == null || selectedIds.isEmpty()) {
			return null;
		}
		Set<Long> normalizedIds = selectedIds.stream()
			.filter(Objects::nonNull)
			.filter(id -> id > 0)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		List<Category> selected = categoryRepository.findByIdIn(normalizedIds);
		boolean invalid = selected.size() != normalizedIds.size()
			|| selected.stream().anyMatch(category -> !CategoryAssignmentTarget.HOSPITAL.accepts(category));
		if (invalid) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 카테고리 정보가 올바르지 않습니다.");
		}
		Set<Long> expanded = new LinkedHashSet<>(normalizedIds);
		for (Category category : selected) {
			String prefix = category.fullPath() + " > ";
			categoryRepository.findByDomainAndGroupCodeAndFullPathStartingWithOrderByDepthAsc(
				CategoryDomain.MEDICAL,
				category.groupCode(),
				prefix
			)
				.stream()
				.filter(child -> child.status() == CategoryStatus.ACTIVE)
				.map(Category::id)
				.forEach(expanded::add);
		}
		return Set.copyOf(expanded);
	}

	private void applyDateRange(
		List<Predicate> predicates,
		jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
		jakarta.persistence.criteria.Path<LocalDateTime> path,
		String startDate,
		String endDate
	) {
		if (StringUtils.hasText(startDate)) {
			predicates.add(criteriaBuilder.greaterThanOrEqualTo(path, parseDate(startDate).atStartOfDay()));
		}
		if (StringUtils.hasText(endDate)) {
			predicates.add(criteriaBuilder.lessThan(path, parseDate(endDate).plusDays(1).atStartOfDay()));
		}
	}

	private Hospital findActiveHospital(Long id) {
		return hospitalRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "병원을 찾을 수 없습니다."));
	}

	private Hospital findLockedActiveHospital(Long id) {
		return hospitalRepository.findForUpdateByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "병원을 찾을 수 없습니다."));
	}

	private Set<HospitalContact> buildContacts(HospitalContactSetCommand contacts, boolean requireRepresentative) {
		Map<HospitalContactType, List<String>> values = new LinkedHashMap<>();
		putSingle(values, HospitalContactType.REPRESENTATIVE_PHONE, contacts.representativePhone());
		putSingle(values, HospitalContactType.SMS_SENDER_PHONE, contacts.smsSenderPhone());
		putSingle(values, HospitalContactType.CALL_RECEIVER_PHONE, contacts.callReceiverPhone());
		putMany(values, HospitalContactType.CONSULTATION_RECEIVER_PHONE, contacts.consultationReceiverPhones());
		putMany(values, HospitalContactType.EVENT_NOTICE_RECEIVER_PHONE, contacts.eventNoticeReceiverPhones());
		putMany(values, HospitalContactType.NOTICE_MARKETING_EMAIL, contacts.noticeMarketingEmails());

		if (requireRepresentative && values.getOrDefault(HospitalContactType.REPRESENTATIVE_PHONE, List.of()).isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "대표 번호는 필수입니다.");
		}

		Set<HospitalContact> result = new LinkedHashSet<>();
		for (Map.Entry<HospitalContactType, List<String>> entry : values.entrySet()) {
			HospitalContactType type = entry.getKey();
			List<String> typedValues = entry.getValue();
			if (typedValues.size() > type.maxCount()) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "연락처 최대 개수를 초과했습니다.", Map.of(
					"type", type.name(),
					"max_count", type.maxCount()
				));
			}
			for (int index = 0; index < typedValues.size(); index++) {
				result.add(new HospitalContact(type, typedValues.get(index), index, index == 0));
			}
		}
		return result;
	}

	private HospitalContactSetCommand mergeContacts(
		Hospital hospital,
		HospitalContactSetCommand requested,
		Set<String> specifiedFields
	) {
		HospitalContactGroupResult current = contacts(hospital);
		return new HospitalContactSetCommand(
			specifiedFields.contains("representative_phone") ? requested.representativePhone() : current.representativePhone(),
			specifiedFields.contains("sms_sender_phone") ? requested.smsSenderPhone() : current.smsSenderPhone(),
			specifiedFields.contains("call_receiver_phone") ? requested.callReceiverPhone() : current.callReceiverPhone(),
			specifiedFields.contains("consultation_receiver_phones")
				? requested.consultationReceiverPhones()
				: current.consultationReceiverPhones(),
			specifiedFields.contains("event_notice_receiver_phones")
				? requested.eventNoticeReceiverPhones()
				: current.eventNoticeReceiverPhones(),
			specifiedFields.contains("notice_marketing_emails")
				? requested.noticeMarketingEmails()
				: current.noticeMarketingEmails()
		);
	}

	private void putSingle(Map<HospitalContactType, List<String>> values, HospitalContactType type, String value) {
		String trimmed = trimToNull(value);
		values.put(type, trimmed == null ? List.of() : List.of(trimmed));
	}

	private void putMany(Map<HospitalContactType, List<String>> values, HospitalContactType type, List<String> rawValues) {
		if (rawValues == null) {
			values.put(type, List.of());
			return;
		}
		values.put(type, rawValues.stream()
			.map(this::trimToNull)
			.filter(Objects::nonNull)
			.distinct()
			.toList());
	}

	private HospitalContactSetCommand requireContacts(HospitalContactSetCommand contacts) {
		if (contacts == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "병원 연락처 정보는 필수입니다.");
		}
		return contacts;
	}

	private HospitalBusinessRegistrationCommand requireBusinessRegistration(HospitalBusinessRegistrationCommand businessRegistration) {
		if (businessRegistration == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "사업자등록 정보는 필수입니다.");
		}
		return businessRegistration;
	}

	private HospitalBusinessRegistration toBusinessRegistration(HospitalBusinessRegistrationCommand command, String businessNumber) {
		return new HospitalBusinessRegistration(
			businessNumber,
			trim(command.companyName()),
			trim(command.ceoName()),
			trim(command.businessType()),
			trim(command.businessItem()),
			trimToNull(command.businessAddress()),
			trimToNull(command.businessAddressDetail()),
			trimToNull(command.settlementBankName()),
			trimToNull(command.settlementAccountNumber()),
			trimToNull(command.settlementAccountHolder()),
			trimToNull(command.taxInvoiceEmail()),
			command.issuedAt()
		);
	}

	private void applyBusinessRegistration(
		Hospital hospital,
		HospitalBusinessRegistrationCommand command,
		String businessNumber,
		Set<String> specifiedFields
	) {
		HospitalBusinessRegistration current = hospital.businessRegistration();
		if (current == null) {
			hospital.replaceBusinessRegistration(toBusinessRegistration(command, businessNumber));
			return;
		}
		current.update(
			businessNumber,
			specifiedFields.contains("company_name") ? trim(command.companyName()) : current.companyName(),
			specifiedFields.contains("ceo_name") ? trim(command.ceoName()) : current.ceoName(),
			specifiedFields.contains("business_type") ? trim(command.businessType()) : current.businessType(),
			specifiedFields.contains("business_item") ? trim(command.businessItem()) : current.businessItem(),
			specifiedFields.contains("business_address") ? trimToNull(command.businessAddress()) : current.businessAddress(),
			specifiedFields.contains("business_address_detail")
				? trimToNull(command.businessAddressDetail())
				: current.businessAddressDetail(),
			specifiedFields.contains("settlement_bank_name")
				? trimToNull(command.settlementBankName())
				: current.settlementBankName(),
			specifiedFields.contains("settlement_account_number")
				? trimToNull(command.settlementAccountNumber())
				: current.settlementAccountNumber(),
			specifiedFields.contains("settlement_account_holder")
				? trimToNull(command.settlementAccountHolder())
				: current.settlementAccountHolder(),
			specifiedFields.contains("tax_invoice_email")
				? trimToNull(command.taxInvoiceEmail())
				: current.taxInvoiceEmail(),
			specifiedFields.contains("issued_at") ? command.issuedAt() : current.issuedAt()
		);
	}

	private void assertBusinessNumberAvailableForUpdate(Hospital hospital, String businessNumber) {
		Optional<HospitalBusinessRegistration> existing = businessRegistrationRepository.findByBusinessNumber(businessNumber);
		if (existing.isPresent()
			&& (hospital.businessRegistration() == null
				|| !Objects.equals(existing.get().id(), hospital.businessRegistration().id()))) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 등록된 사업자등록번호입니다.");
		}
	}

	private Set<HospitalFeature> loadFeatures(Set<Long> featureIds) {
		if (featureIds == null || featureIds.isEmpty()) {
			return Set.of();
		}
		List<HospitalFeature> features = featureRepository.findByIdInAndStatus(featureIds, HospitalFeatureStatus.ACTIVE);
		if (features.size() != featureIds.size()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 병원 특징 정보가 올바르지 않습니다.");
		}
		return new LinkedHashSet<>(features);
	}

	private void syncCategories(Long hospitalId, Set<Long> categoryIds) {
		categoryAssignmentRepository.deleteByCategorizableTypeAndCategorizableId(
			CategoryAssignment.HOSPITAL_TARGET_TYPE,
			hospitalId
		);
		if (categoryIds == null || categoryIds.isEmpty()) {
			return;
		}
		List<Category> categories = categoryRepository.findByIdIn(categoryIds);
		boolean invalidCategory = categories.size() != categoryIds.size()
			|| categories.stream().anyMatch(category -> !CategoryAssignmentTarget.HOSPITAL.accepts(category));
		if (invalidCategory) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 카테고리 정보가 올바르지 않습니다.");
		}
		List<CategoryAssignment> assignments = new ArrayList<>();
		for (int index = 0; index < categories.size(); index++) {
			assignments.add(new CategoryAssignment(
				CategoryAssignment.HOSPITAL_TARGET_TYPE,
				hospitalId,
				categories.get(index),
				index == 0
			));
		}
		categoryAssignmentRepository.saveAll(assignments);
	}

	private HospitalListItemResult toListItem(
		Hospital hospital,
		List<HospitalCategoryResult> categories,
		List<HospitalFeatureResult> features,
		MediaResult logo
	) {
		AccountHospital account = hospital.accountHospital();
		return new HospitalListItemResult(
			hospital.id(),
			hospital.name(),
			contacts(hospital).representativePhone(),
			hospital.viewCount(),
			0,
			new HospitalEvaluationResult(
				hospital.evaluationCount(),
				hospital.evaluationAverageRating().doubleValue()
			),
			new HospitalReviewCountsResult(0, 0),
			hospital.allowStatus().name(),
			hospital.status().name(),
			hospital.createdAt(),
			hospital.updatedAt(),
			logo,
			accountResponse(account),
			categories,
			features
		);
	}

	private HospitalDetailResult toDetail(Hospital hospital, List<HospitalCategoryResult> categories) {
		return toDetail(hospital, categories, Set.of("business_registration"));
	}

	private HospitalDetailResult toDetail(
		Hospital hospital,
		List<HospitalCategoryResult> categories,
		Set<String> include
	) {
		return new HospitalDetailResult(
			hospital.id(),
			hospital.name(),
			hospital.description(),
			hospital.youtubeLink(),
			hospital.address(),
			hospital.addressDetail(),
			hospital.latitude(),
			hospital.longitude(),
			contacts(hospital),
			contactResponses(hospital.contacts()),
			hospital.consultingHours(),
			fromJson(hospital.operationHours()),
			hospital.direction(),
			hospital.viewCount(),
			0,
			hospital.allowStatus().name(),
			hospital.status().name(),
			latestStatusHistory(hospital),
			hospital.createdAt(),
			hospital.updatedAt(),
			mediaReadService.primary(
				MediaOwnerType.HOSPITAL,
				hospital.id(),
				MediaCollectionPolicy.HOSPITAL_LOGO
			),
			mediaReadService.list(
				MediaOwnerType.HOSPITAL,
				hospital.id(),
				MediaCollectionPolicy.HOSPITAL_GALLERY
			),
			categories,
			featureResponses(hospital.features()),
			interpretationLanguageResponses(hospital.interpretationLanguages()),
			include.contains("account_hospital") ? accountResponse(hospital.accountHospital()) : null,
			include.contains("doctors") ? doctorResponses(hospital.id()) : null,
			include.contains("business_registration")
				? businessRegistrationResponse(hospital.businessRegistration())
				: null
		);
	}

	private List<HospitalDoctorForStaffResult> doctorResponses(Long hospitalId) {
		return doctorRepository.findByHospital_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(hospitalId)
			.stream()
			.map(this::doctorResponse)
			.toList();
	}

	private HospitalDoctorForStaffResult doctorResponse(Doctor doctor) {
		return new HospitalDoctorForStaffResult(
			doctor.id(),
			doctor.hospitalId(),
			doctor.name(),
			doctor.position(),
			new DoctorSpecialistResult(
				doctor.specialistField().code(),
				doctor.specialistField().name(),
				doctor.specialistField().label()
			),
			doctor.sortOrder(),
			doctor.allowStatus().name(),
			doctor.status().name(),
			doctor.createdAt(),
			doctor.updatedAt()
		);
	}

	private OperationHistoryResult latestStatusHistory(Hospital hospital) {
		return operationHistoryRepository
			.findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(OperationHistory.TARGET_HOSPITAL, hospital.id())
			.stream()
			.filter(history -> history.changes().stream().anyMatch(change ->
				"status".equals(change.fieldKey()) && hospital.status().name().equals(change.afterValue())))
			.findFirst()
			.map(this::operationHistoryResult)
			.orElse(null);
	}

	private OperationHistoryResult operationHistoryResult(OperationHistory history) {
		return new OperationHistoryResult(
			history.id(),
			history.action(),
			history.reason(),
			history.createdAt(),
			history.changes().stream()
				.map(change -> new OperationHistoryChangeResult(
					change.fieldKey(),
					change.beforeValue(),
					change.afterValue()
				))
				.toList()
		);
	}

	private HospitalAccountResult accountResponse(AccountHospital account) {
		if (account == null) {
			return null;
		}
		return new HospitalAccountResult(
			account.id(),
			account.name(),
			account.nickname(),
			account.email(),
			account.phone(),
			account.status().name(),
			account.lastLoginAt(),
			account.createdAt(),
			account.updatedAt()
		);
	}

	private HospitalBusinessRegistrationResult businessRegistrationResponse(HospitalBusinessRegistration registration) {
		if (registration == null) {
			return null;
		}
		return new HospitalBusinessRegistrationResult(
			registration.id(),
			registration.businessNumber(),
			registration.companyName(),
			registration.ceoName(),
			registration.businessType(),
			registration.businessItem(),
			registration.businessAddress(),
			registration.businessAddressDetail(),
			new HospitalSettlementAccountResult(
				registration.settlementBankName(),
				registration.settlementAccountNumber(),
				registration.settlementAccountHolder(),
				registration.taxInvoiceEmail()
			),
			registration.issuedAt(),
			registration.status().name(),
			mediaReadService.primary(
				MediaOwnerType.HOSPITAL_BUSINESS_REGISTRATION,
				registration.id(),
				MediaCollectionPolicy.HOSPITAL_BUSINESS_REGISTRATION_FILE
			)
		);
	}

	private HospitalContactGroupResult contacts(Hospital hospital) {
		Map<HospitalContactType, List<String>> byType = hospital.contacts().stream()
			.filter(HospitalContact::active)
			.sorted(Comparator.comparing(HospitalContact::sortOrder).thenComparing(HospitalContact::id))
			.collect(Collectors.groupingBy(
				HospitalContact::contactType,
				LinkedHashMap::new,
				Collectors.mapping(HospitalContact::value, Collectors.toList())
			));
		return new HospitalContactGroupResult(
			first(byType.get(HospitalContactType.REPRESENTATIVE_PHONE)),
			first(byType.get(HospitalContactType.SMS_SENDER_PHONE)),
			first(byType.get(HospitalContactType.CALL_RECEIVER_PHONE)),
			byType.getOrDefault(HospitalContactType.CONSULTATION_RECEIVER_PHONE, List.of()),
			byType.getOrDefault(HospitalContactType.EVENT_NOTICE_RECEIVER_PHONE, List.of()),
			byType.getOrDefault(HospitalContactType.NOTICE_MARKETING_EMAIL, List.of())
		);
	}

	private List<HospitalContactResult> contactResponses(Set<HospitalContact> contacts) {
		return contacts.stream()
			.sorted(Comparator.comparing(HospitalContact::contactType).thenComparing(HospitalContact::sortOrder))
			.map(contact -> new HospitalContactResult(
				contact.id(),
				contact.contactType().name(),
				contact.value(),
				contact.sortOrder(),
				contact.primary(),
				contact.active()
			))
			.toList();
	}

	private List<HospitalFeatureResult> featureResponses(Set<HospitalFeature> features) {
		return features.stream()
			.sorted(Comparator.comparing(HospitalFeature::sortOrder).thenComparing(HospitalFeature::id))
			.map(feature -> new HospitalFeatureResult(
				feature.id(),
				feature.code(),
				feature.name(),
				feature.sortOrder(),
				feature.status().name()
			))
			.toList();
	}

	private List<HospitalInterpretationLanguageResult> interpretationLanguageResponses(
		Set<HospitalInterpretationLanguage> languages
	) {
		return languages.stream()
			.sorted(Comparator.comparingInt(Enum::ordinal))
			.map(language -> new HospitalInterpretationLanguageResult(language.name(), language.label()))
			.toList();
	}

	private Map<Long, List<HospitalCategoryResult>> categoriesByHospitalIds(List<Hospital> hospitals) {
		List<Long> hospitalIds = hospitals.stream().map(Hospital::id).toList();
		if (hospitalIds.isEmpty()) {
			return Map.of();
		}
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableIdIn(CategoryAssignment.HOSPITAL_TARGET_TYPE, hospitalIds)
			.stream()
			.sorted(Comparator.comparing((CategoryAssignment assignment) -> !assignment.primary())
				.thenComparing(assignment -> assignment.category().sortOrder())
				.thenComparing(assignment -> assignment.category().id()))
			.collect(Collectors.groupingBy(
				CategoryAssignment::categorizableId,
				LinkedHashMap::new,
				Collectors.mapping(
					assignment -> categoryResponse(assignment.category(), assignment.primary()),
					Collectors.toList()
				)
			));
	}

	private List<HospitalCategoryResult> categories(Long hospitalId) {
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableId(CategoryAssignment.HOSPITAL_TARGET_TYPE, hospitalId)
			.stream()
			.sorted(Comparator.comparing((CategoryAssignment assignment) -> !assignment.primary())
				.thenComparing(assignment -> assignment.category().sortOrder())
				.thenComparing(assignment -> assignment.category().id()))
			.map(assignment -> categoryResponse(assignment.category(), assignment.primary()))
			.toList();
	}

	private HospitalCategoryResult categoryResponse(Category category, boolean primary) {
		return new HospitalCategoryResult(
			category.id(),
			category.domain().name(),
			category.parentId(),
			category.name(),
			category.fullPath(),
			category.depth(),
			category.sortOrder(),
			primary
		);
	}

	private Map<String, String> capture(Hospital hospital) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("description", hospital.description());
		values.put("youtube_link", hospital.youtubeLink());
		values.put("address", hospital.address());
		values.put("address_detail", hospital.addressDetail());
		values.put("latitude", hospital.latitude());
		values.put("longitude", hospital.longitude());
		values.put("consulting_hours", hospital.consultingHours());
		values.put("operation_hours", hospital.operationHours());
		values.put("direction", hospital.direction());
		values.put("allow_status", hospital.allowStatus().name());
		values.put("status", hospital.status().name());
		values.put("contacts", writeInternalJson(contacts(hospital)));
		HospitalBusinessRegistration registration = hospital.businessRegistration();
		if (registration != null) {
			Map<String, Object> business = new LinkedHashMap<>();
			business.put("business_number", registration.businessNumber());
			business.put("company_name", registration.companyName());
			business.put("ceo_name", registration.ceoName());
			business.put("business_type", registration.businessType());
			business.put("business_item", registration.businessItem());
			business.put("business_address", registration.businessAddress());
			business.put("business_address_detail", registration.businessAddressDetail());
			business.put("settlement_bank_name", registration.settlementBankName());
			business.put("settlement_account_number", registration.settlementAccountNumber());
			business.put("settlement_account_holder", registration.settlementAccountHolder());
			business.put("tax_invoice_email", registration.taxInvoiceEmail());
			business.put("issued_at", registration.issuedAt());
			values.put("business_registration", writeInternalJson(business));
			values.put("business_registration_file", writeInternalJson(mediaSnapshot(mediaReadService.primary(
				MediaOwnerType.HOSPITAL_BUSINESS_REGISTRATION,
				registration.id(),
				MediaCollectionPolicy.HOSPITAL_BUSINESS_REGISTRATION_FILE
			))));
		}
		values.put("categories", writeInternalJson(categories(hospital.id())));
		values.put("features", writeInternalJson(featureResponses(hospital.features())));
		values.put(
			"interpretation_languages",
			writeInternalJson(interpretationLanguageResponses(hospital.interpretationLanguages()))
		);
		values.put("logo", writeInternalJson(mediaSnapshot(mediaReadService.primary(
			MediaOwnerType.HOSPITAL,
			hospital.id(),
			MediaCollectionPolicy.HOSPITAL_LOGO
		))));
		values.put("gallery", writeInternalJson(mediaReadService.list(
			MediaOwnerType.HOSPITAL,
			hospital.id(),
			MediaCollectionPolicy.HOSPITAL_GALLERY
		).stream().map(this::mediaSnapshot).toList()));
		return values;
	}

	private Map<String, Object> mediaSnapshot(MediaResult media) {
		if (media == null) {
			return null;
		}
		Map<String, Object> value = new LinkedHashMap<>();
		value.put("id", media.id());
		value.put("name", media.originalName());
		value.put("mime_type", media.mimeType());
		value.put("size", media.size());
		return value;
	}

	private String writeInternalJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("병원 변경 이력 JSON을 만들 수 없습니다.", exception);
		}
	}

	private void recordSimpleHistory(
		AuthenticatedActor actor,
		Hospital hospital,
		String action,
		String reason
	) {
		operationHistoryRepository.save(new OperationHistory(
			OperationHistory.TARGET_HOSPITAL,
			hospital.id(),
			actor.actorType().name(),
			actor.accountId(),
			action,
			trimToNull(reason),
			null
		));
	}

	private void recordChangedHistory(
		AuthenticatedActor actor,
		Hospital hospital,
		String action,
		String reason,
		Map<String, String> before,
		Map<String, String> after
	) {
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_HOSPITAL,
			hospital.id(),
			actor.actorType().name(),
			actor.accountId(),
			action,
			trimToNull(reason),
			null
		);
		before.forEach((key, beforeValue) -> {
			String afterValue = after.get(key);
			if (!Objects.equals(beforeValue, afterValue)) {
				history.addChange(key, beforeValue, afterValue);
			}
		});
		if (!history.changes().isEmpty()) {
			operationHistoryRepository.save(history);
		}
	}

	private String normalizeOperationHours(Object value) {
		if (value == null || value instanceof String stringValue && stringValue.isBlank()) {
			return null;
		}
		try {
			JsonNode root = value instanceof String rawValue
				? objectMapper.readTree(rawValue)
				: objectMapper.valueToTree(value);
			if (root == null || !root.isObject()) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "진료시간 형식이 올바르지 않습니다.");
			}
			for (String day : List.of("mon", "tue", "wed", "thu", "fri", "sat", "sun")) {
				JsonNode hours = root.get(day);
				if (hours == null || !hours.isObject() || !hours.path("is_closed").isBoolean()) {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "요일별 진료시간을 모두 입력해주세요.");
				}
				if (hours.path("is_closed").booleanValue()) {
					continue;
				}
				String start = hours.path("start").isTextual() ? hours.path("start").textValue() : null;
				String end = hours.path("end").isTextual() ? hours.path("end").textValue() : null;
				if (start == null || end == null || !start.matches("^\\d{2}:\\d{2}$") || !end.matches("^\\d{2}:\\d{2}$")) {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "진료 시작 시간과 종료 시간을 HH:mm 형식으로 입력해주세요.");
				}
				LocalTime startTime = LocalTime.parse(start);
				LocalTime endTime = LocalTime.parse(end);
				if (!endTime.isAfter(startTime)) {
					throw new ApiException(ErrorCode.INVALID_REQUEST, "진료 종료 시간은 시작 시간보다 늦어야 합니다.");
				}
			}
			return objectMapper.writeValueAsString(root);
		} catch (ApiException exception) {
			throw exception;
		} catch (JsonProcessingException | IllegalArgumentException | DateTimeParseException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "진료시간 형식이 올바르지 않습니다.");
		}
	}

	private String normalizeYoutubeLink(String value) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			URI uri = new URI(normalized);
			String scheme = uri.getScheme();
			String host = uri.getHost();
			if (scheme == null || host == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "유튜브 링크 형식이 올바르지 않습니다.");
			}
			String normalizedHost = host.toLowerCase(java.util.Locale.ROOT).replaceFirst("^www\\.", "");
			if (!(normalizedHost.equals("youtube.com")
				|| normalizedHost.endsWith(".youtube.com")
				|| normalizedHost.equals("youtu.be"))) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "유튜브 링크 형식이 올바르지 않습니다.");
			}
			return normalized;
		} catch (URISyntaxException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "유튜브 링크 형식이 올바르지 않습니다.");
		}
	}

	private Object fromJson(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, Object.class);
		} catch (JsonProcessingException exception) {
			throw new InternalApplicationException("저장된 병원 진료시간 JSON이 올바르지 않습니다.", exception);
		}
	}

	private String normalizeBusinessNumber(String value) {
		String trimmed = trim(value);
		String normalized = trimmed.replaceAll("\\D+", "");
		return normalized.isBlank() ? trimmed : normalized;
	}

	private LocalDate parseDate(String value) {
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "날짜 형식은 yyyy-MM-dd 이어야 합니다.");
		}
	}

	private Optional<Long> parseLong(String value) {
		String normalized = value.replaceAll("(?i)^(HID|UID)[-_ ]?", "");
		if (!normalized.matches("\\d+")) {
			return Optional.empty();
		}
		return Optional.of(Long.parseLong(normalized));
	}

	private String trim(String value) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "필수 값이 누락되었습니다.");
		}
		return trimmed;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String first(List<String> values) {
		return values == null || values.isEmpty() ? null : values.getFirst();
	}

	private int clamp(int value, int min, int max) {
		return Math.min(Math.max(value, min), max);
	}
}
