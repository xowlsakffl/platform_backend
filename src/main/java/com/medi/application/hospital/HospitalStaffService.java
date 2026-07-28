package com.medi.application.hospital;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.auth.PermissionService;
import com.medi.application.hospital.command.CreateHospitalCommand;
import com.medi.application.hospital.command.ChangeHospitalAllowStatusCommand;
import com.medi.application.hospital.command.ChangeHospitalStatusCommand;
import com.medi.application.hospital.command.HospitalBusinessRegistrationCommand;
import com.medi.application.hospital.command.HospitalContactSetCommand;
import com.medi.application.hospital.command.UpdateHospitalCommand;
import com.medi.application.hospital.query.SearchHospitalsQuery;
import com.medi.application.hospital.result.DuplicateCheckResult;
import com.medi.application.hospital.result.HospitalAccountResult;
import com.medi.application.hospital.result.HospitalAllowStatusBulkUpdateResult;
import com.medi.application.hospital.result.HospitalBusinessRegistrationResult;
import com.medi.application.hospital.result.HospitalCategoryResult;
import com.medi.application.hospital.result.HospitalContactGroupResult;
import com.medi.application.hospital.result.HospitalContactResult;
import com.medi.application.hospital.result.HospitalDeletedResult;
import com.medi.application.hospital.result.HospitalDetailResult;
import com.medi.application.hospital.result.HospitalFeatureResult;
import com.medi.application.hospital.result.HospitalListItemResult;
import com.medi.application.hospital.result.HospitalSummaryResult;
import com.medi.application.hospital.result.OperationHistoryChangeResult;
import com.medi.application.hospital.result.OperationHistoryResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.PaginatedResponse;
import com.medi.domain.account.AccountHospital;
import com.medi.domain.category.Category;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.hospital.Hospital;
import com.medi.domain.hospital.HospitalAllowStatus;
import com.medi.domain.hospital.HospitalBusinessRegistration;
import com.medi.domain.hospital.HospitalContact;
import com.medi.domain.hospital.HospitalContactType;
import com.medi.domain.hospital.HospitalFeature;
import com.medi.domain.hospital.HospitalFeatureStatus;
import com.medi.domain.hospital.HospitalStatus;
import com.medi.domain.operationhistory.OperationHistory;
import com.medi.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.medi.infrastructure.persistence.category.CategoryRepository;
import com.medi.infrastructure.persistence.hospital.HospitalBusinessRegistrationRepository;
import com.medi.infrastructure.persistence.hospital.HospitalFeatureRepository;
import com.medi.infrastructure.persistence.hospital.HospitalRepository;
import com.medi.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class HospitalStaffService {

	private static final String ACTION_CREATED = "ACTION_CREATED";
	private static final String ACTION_UPDATED = "ACTION_UPDATED";
	private static final String ACTION_STATUS_UPDATED = "ACTION_STATUS_UPDATED";
	private static final String ACTION_ALLOW_STATUS_UPDATED = "ACTION_ALLOW_STATUS_UPDATED";
	private static final String ACTION_DELETED = "ACTION_DELETED";
	private static final String PERMISSION_SHOW = "platform.hospital.show";
	private static final String PERMISSION_CREATE = "platform.hospital.create";
	private static final String PERMISSION_UPDATE = "platform.hospital.update";
	private static final String PERMISSION_DELETE = "platform.hospital.delete";

	private final PermissionService permissionService;
	private final HospitalRepository hospitalRepository;
	private final HospitalBusinessRegistrationRepository businessRegistrationRepository;
	private final HospitalFeatureRepository featureRepository;
	private final CategoryRepository categoryRepository;
	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final OperationHistoryRepository operationHistoryRepository;
	private final ObjectMapper objectMapper;

	public HospitalStaffService(
		PermissionService permissionService,
		HospitalRepository hospitalRepository,
		HospitalBusinessRegistrationRepository businessRegistrationRepository,
		HospitalFeatureRepository featureRepository,
		CategoryRepository categoryRepository,
		CategoryAssignmentRepository categoryAssignmentRepository,
		OperationHistoryRepository operationHistoryRepository,
		ObjectMapper objectMapper
	) {
		this.permissionService = permissionService;
		this.hospitalRepository = hospitalRepository;
		this.businessRegistrationRepository = businessRegistrationRepository;
		this.featureRepository = featureRepository;
		this.categoryRepository = categoryRepository;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
		this.operationHistoryRepository = operationHistoryRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<HospitalListItemResult> list(AuthenticatedActor actor, SearchHospitalsQuery condition) {
		permissionService.requireStaffPermission(actor, PERMISSION_SHOW);
		Pageable pageable = PageRequest.of(
			Math.max(condition.page(), 1) - 1,
			clamp(condition.perPage(), 1, 100),
			sort(condition)
		);
		Page<Hospital> page = hospitalRepository.findAll(specification(condition), pageable);
		Map<Long, List<HospitalCategoryResult>> categories = categoriesByHospitalIds(page.getContent());

		return PaginatedResponse.from(page, hospital -> toListItem(hospital, categories.getOrDefault(hospital.id(), List.of())));
	}

	@Transactional(readOnly = true)
	public HospitalSummaryResult summary(AuthenticatedActor actor) {
		permissionService.requireStaffPermission(actor, PERMISSION_SHOW);
		return new HospitalSummaryResult(
			hospitalRepository.countByDeletedAtIsNull(),
			hospitalRepository.countByDeletedAtIsNullAndAllowStatus(HospitalAllowStatus.PENDING),
			hospitalRepository.countByDeletedAtIsNullAndAllowStatus(HospitalAllowStatus.APPROVED),
			hospitalRepository.countByDeletedAtIsNullAndAllowStatus(HospitalAllowStatus.REJECTED),
			hospitalRepository.countByDeletedAtIsNullAndStatus(HospitalStatus.ACTIVE),
			hospitalRepository.countByDeletedAtIsNullAndStatus(HospitalStatus.SUSPENDED),
			hospitalRepository.countByDeletedAtIsNullAndStatus(HospitalStatus.WITHDRAWN)
		);
	}

	@Transactional(readOnly = true)
	public HospitalDetailResult get(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, PERMISSION_SHOW);
		Hospital hospital = findActiveHospital(id);
		return toDetail(hospital, categories(hospital.id()));
	}

	@Transactional(readOnly = true)
	public List<OperationHistoryResult> histories(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, PERMISSION_SHOW);
		findActiveHospital(id);
		return operationHistoryRepository
			.findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(OperationHistory.TARGET_HOSPITAL, id)
			.stream()
			.map(history -> new OperationHistoryResult(
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
			))
			.toList();
	}

	@Transactional
	public HospitalDetailResult create(AuthenticatedActor actor, CreateHospitalCommand command) {
		permissionService.requireStaffPermission(actor, PERMISSION_CREATE);
		if (hospitalRepository.existsByNameAndDeletedAtIsNull(command.name())) {
			throw new ApiException(ErrorCode.CONFLICT, "이미 등록된 병의원명입니다.");
		}
		HospitalBusinessRegistrationCommand businessCommand = requireBusinessRegistration(command.businessRegistration());
		String businessNumber = normalizeBusinessNumber(businessCommand.businessNumber());
		if (businessRegistrationRepository.existsByBusinessNumber(businessNumber)) {
			throw new ApiException(ErrorCode.CONFLICT, "이미 등록된 사업자등록번호입니다.");
		}

		Hospital hospital = new Hospital(
			trim(command.name()),
			command.department(),
			trimToNull(command.description()),
			trimToNull(command.youtubeLink()),
			trimToNull(command.address()),
			trimToNull(command.addressDetail()),
			trimToNull(command.latitude()),
			trimToNull(command.longitude()),
			trimToNull(command.consultingHours()),
			toJson(command.operationHours()),
			trimToNull(command.direction()),
			command.allowStatus(),
			command.status()
		);
		hospital.replaceContacts(buildContacts(requireContacts(command.contacts()), true));
		hospital.replaceBusinessRegistration(toBusinessRegistration(businessCommand, businessNumber));
		hospital.replaceFeatures(loadFeatures(command.featureIds()));

		Hospital saved = hospitalRepository.saveAndFlush(hospital);
		syncCategories(saved.id(), command.categoryIds());
		recordSimpleHistory(saved, ACTION_CREATED, null);

		return toDetail(findActiveHospital(saved.id()), categories(saved.id()));
	}

	@Transactional
	public HospitalDetailResult update(AuthenticatedActor actor, Long id, UpdateHospitalCommand command) {
		permissionService.requireStaffPermission(actor, PERMISSION_UPDATE);
		Hospital hospital = findActiveHospital(id);
		Map<String, String> before = capture(hospital);

		if (command.contacts() != null) {
			hospital.replaceContacts(buildContacts(command.contacts(), false));
		}
		if (command.businessRegistration() != null) {
			HospitalBusinessRegistrationCommand businessCommand = command.businessRegistration();
			String businessNumber = normalizeBusinessNumber(businessCommand.businessNumber());
			assertBusinessNumberAvailableForUpdate(hospital, businessNumber);
			applyBusinessRegistration(hospital, businessCommand, businessNumber);
		}
		if (command.featureIds() != null) {
			hospital.replaceFeatures(loadFeatures(command.featureIds()));
		}

		hospital.updateProfile(
			command.department(),
			command.description() == null ? hospital.description() : trimToNull(command.description()),
			command.youtubeLink() == null ? hospital.youtubeLink() : trimToNull(command.youtubeLink()),
			command.address() == null ? hospital.address() : trimToNull(command.address()),
			command.addressDetail() == null ? hospital.addressDetail() : trimToNull(command.addressDetail()),
			command.latitude() == null ? hospital.latitude() : trimToNull(command.latitude()),
			command.longitude() == null ? hospital.longitude() : trimToNull(command.longitude()),
			command.consultingHours() == null ? hospital.consultingHours() : trimToNull(command.consultingHours()),
			command.operationHours() == null ? hospital.operationHours() : toJson(command.operationHours()),
			command.direction() == null ? hospital.direction() : trimToNull(command.direction()),
			command.allowStatus(),
			command.status()
		);
		Hospital saved = hospitalRepository.saveAndFlush(hospital);

		if (command.categoryIds() != null) {
			syncCategories(saved.id(), command.categoryIds());
		}
		recordChangedHistory(saved, ACTION_UPDATED, null, before, capture(saved));

		return toDetail(findActiveHospital(saved.id()), categories(saved.id()));
	}

	@Transactional
	public HospitalDetailResult changeStatus(AuthenticatedActor actor, Long id, ChangeHospitalStatusCommand command) {
		permissionService.requireStaffPermission(actor, PERMISSION_UPDATE);
		Hospital hospital = findActiveHospital(id);
		HospitalStatus before = hospital.status();
		hospital.changeStatus(command.status());
		Hospital saved = hospitalRepository.saveAndFlush(hospital);

		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_HOSPITAL,
			saved.id(),
			ACTION_STATUS_UPDATED,
			trimToNull(command.reason()),
			null
		);
		history.addChange("status", before.name(), command.status().name());
		operationHistoryRepository.save(history);

		return toDetail(findActiveHospital(saved.id()), categories(saved.id()));
	}

	@Transactional
	public HospitalAllowStatusBulkUpdateResult changeAllowStatus(
		AuthenticatedActor actor,
		ChangeHospitalAllowStatusCommand command
	) {
		permissionService.requireStaffPermission(actor, PERMISSION_UPDATE);
		List<Long> normalizedIds = command.ids().stream()
			.filter(Objects::nonNull)
			.filter(id -> id > 0)
			.distinct()
			.toList();
		if (normalizedIds.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "변경할 병의원을 선택해주세요.");
		}
		List<Hospital> hospitals = hospitalRepository.findByIdInAndDeletedAtIsNull(normalizedIds);
		if (hospitals.isEmpty()) {
			throw new ApiException(ErrorCode.NOT_FOUND, "변경할 병의원을 찾을 수 없습니다.");
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
				ACTION_ALLOW_STATUS_UPDATED,
				trimToNull(command.reason()),
				null
			);
			history.addChange("allow_status", before.name(), command.allowStatus().name());
			operationHistoryRepository.save(history);
			updatedCount++;
		}
		hospitalRepository.saveAll(hospitals);

		return new HospitalAllowStatusBulkUpdateResult(
			updatedCount,
			command.allowStatus().name(),
			hospitals.stream().map(Hospital::id).toList()
		);
	}

	@Transactional
	public HospitalDeletedResult delete(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, PERMISSION_DELETE);
		Hospital hospital = findActiveHospital(id);
		hospital.softDelete();
		Hospital saved = hospitalRepository.saveAndFlush(hospital);
		recordSimpleHistory(saved, ACTION_DELETED, null);
		return new HospitalDeletedResult(saved.id(), saved.deletedAt());
	}

	@Transactional(readOnly = true)
	public DuplicateCheckResult checkName(AuthenticatedActor actor, String name) {
		permissionService.requireStaffPermission(actor, PERMISSION_CREATE);
		return new DuplicateCheckResult(hospitalRepository.existsByNameAndDeletedAtIsNull(trim(name)));
	}

	@Transactional(readOnly = true)
	public DuplicateCheckResult checkBusinessNumber(AuthenticatedActor actor, String businessNumber) {
		permissionService.requireStaffPermission(actor, PERMISSION_CREATE);
		return new DuplicateCheckResult(
			businessRegistrationRepository.existsByBusinessNumber(normalizeBusinessNumber(businessNumber))
		);
	}

	private Specification<Hospital> specification(SearchHospitalsQuery condition) {
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
			if (condition.allowStatus() != null && !condition.allowStatus().isEmpty()) {
				predicates.add(root.get("allowStatus").in(condition.allowStatus()));
			}
			if (condition.department() != null && !condition.department().isEmpty()) {
				predicates.add(root.get("department").in(condition.department()));
			}
			if (Boolean.TRUE.equals(condition.dormant())) {
				var accountJoin = root.join("accountHospital", JoinType.LEFT);
				predicates.add(criteriaBuilder.notEqual(root.get("status"), HospitalStatus.WITHDRAWN));
				predicates.add(criteriaBuilder.or(
					criteriaBuilder.isNull(accountJoin.get("lastLoginAt")),
					criteriaBuilder.lessThan(accountJoin.<LocalDateTime>get("lastLoginAt"), LocalDateTime.now().minusDays(30))
				));
			}
			applyDateRange(predicates, criteriaBuilder, root.<LocalDateTime>get("createdAt"), condition.startDate(), condition.endDate());
			applyDateRange(predicates, criteriaBuilder, root.<LocalDateTime>get("updatedAt"), condition.updatedStartDate(), condition.updatedEndDate());
			if (condition.categoryIds() != null && !condition.categoryIds().isEmpty()) {
				Subquery<Long> subquery = query.subquery(Long.class);
				var assignment = subquery.from(CategoryAssignment.class);
				subquery.select(assignment.get("categorizableId"))
					.where(
						criteriaBuilder.equal(assignment.get("categorizableType"), CategoryAssignment.HOSPITAL_TARGET_TYPE),
						assignment.get("category").get("id").in(condition.categoryIds())
					);
				predicates.add(root.get("id").in(subquery));
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
			case "name" -> Sort.by(direction, "name");
			case "updated_at" -> Sort.by(direction, "updatedAt");
			case "created_at" -> Sort.by(direction, "createdAt");
			case "view_count" -> Sort.by(direction, "viewCount");
			case "evaluation_count" -> Sort.by(direction, "evaluationCount");
			case "evaluation_average_rating" -> Sort.by(direction, "evaluationAverageRating");
			case "status" -> Sort.by(direction, "status");
			case "allow_status" -> Sort.by(direction, "allowStatus");
			default -> Sort.unsorted();
		};
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
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "병의원을 찾을 수 없습니다."));
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
			trimToNull(command.businessType()),
			trimToNull(command.businessItem()),
			trimToNull(command.businessAddress()),
			trimToNull(command.businessAddressDetail()),
			trimToNull(command.settlementBankName()),
			trimToNull(command.settlementAccountNumber()),
			trimToNull(command.settlementAccountHolder()),
			trimToNull(command.taxInvoiceEmail()),
			command.issuedAt()
		);
	}

	private void applyBusinessRegistration(Hospital hospital, HospitalBusinessRegistrationCommand command, String businessNumber) {
		HospitalBusinessRegistration current = hospital.businessRegistration();
		if (current == null) {
			hospital.replaceBusinessRegistration(toBusinessRegistration(command, businessNumber));
			return;
		}
		current.update(
			businessNumber,
			trim(command.companyName()),
			trim(command.ceoName()),
			trimToNull(command.businessType()),
			trimToNull(command.businessItem()),
			trimToNull(command.businessAddress()),
			trimToNull(command.businessAddressDetail()),
			trimToNull(command.settlementBankName()),
			trimToNull(command.settlementAccountNumber()),
			trimToNull(command.settlementAccountHolder()),
			trimToNull(command.taxInvoiceEmail()),
			command.issuedAt()
		);
	}

	private void assertBusinessNumberAvailableForUpdate(Hospital hospital, String businessNumber) {
		Optional<HospitalBusinessRegistration> existing = businessRegistrationRepository.findByBusinessNumber(businessNumber);
		if (existing.isPresent()
			&& (hospital.businessRegistration() == null
				|| !Objects.equals(existing.get().id(), hospital.businessRegistration().id()))) {
			throw new ApiException(ErrorCode.CONFLICT, "이미 등록된 사업자등록번호입니다.");
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
		if (categories.size() != categoryIds.size()) {
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

	private HospitalListItemResult toListItem(Hospital hospital, List<HospitalCategoryResult> categories) {
		AccountHospital account = hospital.accountHospital();
		return new HospitalListItemResult(
			hospital.id(),
			hospital.name(),
			hospital.department().name(),
			hospital.department().label(),
			contacts(hospital).representativePhone(),
			hospital.viewCount(),
			0,
			hospital.evaluationCount(),
			hospital.evaluationAverageRating().doubleValue(),
			0,
			0,
			hospital.allowStatus().name(),
			hospital.allowStatus().label(),
			hospital.status().name(),
			hospital.status().label(),
			hospital.createdAt(),
			hospital.updatedAt(),
			accountResponse(account),
			categories,
			featureResponses(hospital.features())
		);
	}

	private HospitalDetailResult toDetail(Hospital hospital, List<HospitalCategoryResult> categories) {
		return new HospitalDetailResult(
			hospital.id(),
			hospital.name(),
			hospital.department().name(),
			hospital.department().label(),
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
			hospital.evaluationCount(),
			hospital.evaluationAverageRating().doubleValue(),
			hospital.allowStatus().name(),
			hospital.allowStatus().label(),
			hospital.status().name(),
			hospital.status().label(),
			hospital.createdAt(),
			hospital.updatedAt(),
			businessRegistrationResponse(hospital.businessRegistration()),
			accountResponse(hospital.accountHospital()),
			categories,
			featureResponses(hospital.features())
		);
	}

	private HospitalAccountResult accountResponse(AccountHospital account) {
		if (account == null) {
			return null;
		}
		return new HospitalAccountResult(
			account.id(),
			account.nickname(),
			account.email(),
			account.phone(),
			account.status().name(),
			account.lastLoginAt()
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
			registration.settlementBankName(),
			registration.settlementAccountNumber(),
			registration.settlementAccountHolder(),
			registration.taxInvoiceEmail(),
			registration.issuedAt(),
			registration.status().name()
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

	private Map<Long, List<HospitalCategoryResult>> categoriesByHospitalIds(List<Hospital> hospitals) {
		List<Long> hospitalIds = hospitals.stream().map(Hospital::id).toList();
		if (hospitalIds.isEmpty()) {
			return Map.of();
		}
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableIdIn(CategoryAssignment.HOSPITAL_TARGET_TYPE, hospitalIds)
			.stream()
			.collect(Collectors.groupingBy(
				CategoryAssignment::categorizableId,
				Collectors.mapping(assignment -> categoryResponse(assignment.category()), Collectors.toList())
			));
	}

	private List<HospitalCategoryResult> categories(Long hospitalId) {
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableId(CategoryAssignment.HOSPITAL_TARGET_TYPE, hospitalId)
			.stream()
			.map(CategoryAssignment::category)
			.sorted(Comparator.comparing(Category::depth).thenComparing(Category::sortOrder).thenComparing(Category::id))
			.map(this::categoryResponse)
			.toList();
	}

	private HospitalCategoryResult categoryResponse(Category category) {
		return new HospitalCategoryResult(
			category.id(),
			category.name(),
			category.fullPath(),
			category.depth(),
			category.sortOrder()
		);
	}

	private Map<String, String> capture(Hospital hospital) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("department", hospital.department().name());
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
		return values;
	}

	private void recordSimpleHistory(Hospital hospital, String action, String reason) {
		operationHistoryRepository.save(new OperationHistory(
			OperationHistory.TARGET_HOSPITAL,
			hospital.id(),
			action,
			trimToNull(reason),
			null
		));
	}

	private void recordChangedHistory(
		Hospital hospital,
		String action,
		String reason,
		Map<String, String> before,
		Map<String, String> after
	) {
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_HOSPITAL,
			hospital.id(),
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

	private String toJson(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String stringValue) {
			return trimToNull(stringValue);
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "진료시간 형식이 올바르지 않습니다.");
		}
	}

	private Object fromJson(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, Object.class);
		} catch (JsonProcessingException exception) {
			return json;
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
		} catch (RuntimeException exception) {
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
