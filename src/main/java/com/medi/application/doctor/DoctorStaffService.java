package com.medi.application.doctor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medi.application.auth.PermissionService;
import com.medi.application.doctor.command.PatchDoctorCommand;
import com.medi.application.doctor.command.SaveDoctorCommand;
import com.medi.application.doctor.query.SearchDoctorsQuery;
import com.medi.application.doctor.result.DoctorCategoryResult;
import com.medi.application.doctor.result.DoctorDeletedResult;
import com.medi.application.doctor.result.DoctorDetailResult;
import com.medi.application.doctor.result.DoctorHospitalOptionResult;
import com.medi.application.doctor.result.DoctorListItemResult;
import com.medi.application.doctor.result.DoctorMediaResult;
import com.medi.application.doctor.result.DoctorSpecialistResult;
import com.medi.application.media.MediaCollectionPolicy;
import com.medi.application.media.MediaCommandService;
import com.medi.application.media.MediaLifecycleService;
import com.medi.application.media.MediaReadService;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.PaginatedResponse;
import com.medi.domain.category.Category;
import com.medi.domain.category.CategoryAssignment;
import com.medi.domain.category.CategoryDomain;
import com.medi.domain.category.CategoryStatus;
import com.medi.domain.category.CategoryUsageType;
import com.medi.domain.doctor.Doctor;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorSpecialistField;
import com.medi.domain.hospital.Hospital;
import com.medi.domain.media.MediaOwnerType;
import com.medi.domain.operationhistory.OperationHistory;
import com.medi.infrastructure.persistence.category.CategoryAssignmentRepository;
import com.medi.infrastructure.persistence.category.CategoryRepository;
import com.medi.infrastructure.persistence.category.CategoryUsageRepository;
import com.medi.infrastructure.persistence.doctor.DoctorRepository;
import com.medi.infrastructure.persistence.hospital.HospitalRepository;
import com.medi.infrastructure.persistence.operationhistory.OperationHistoryRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.springframework.util.StringUtils;

@Service
public class DoctorStaffService {

	private static final String PERMISSION_SHOW = "platform.doctor.show";
	private static final String PERMISSION_CREATE = "platform.doctor.create";
	private static final String PERMISSION_UPDATE = "platform.doctor.update";
	private static final String PERMISSION_DELETE = "platform.doctor.delete";
	private static final int MAX_CATEGORY_COUNT = 5;
	private static final int MAX_TEXT_ITEM_COUNT = 20;
	private static final int MAX_TEXT_ITEM_LENGTH = 1_000;
	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

	private final PermissionService permissionService;
	private final DoctorRepository doctorRepository;
	private final HospitalRepository hospitalRepository;
	private final CategoryRepository categoryRepository;
	private final CategoryUsageRepository categoryUsageRepository;
	private final CategoryAssignmentRepository categoryAssignmentRepository;
	private final OperationHistoryRepository operationHistoryRepository;
	private final MediaCommandService mediaCommandService;
	private final MediaReadService mediaReadService;
	private final MediaLifecycleService mediaLifecycleService;
	private final ObjectMapper objectMapper;

	public DoctorStaffService(
		PermissionService permissionService,
		DoctorRepository doctorRepository,
		HospitalRepository hospitalRepository,
		CategoryRepository categoryRepository,
		CategoryUsageRepository categoryUsageRepository,
		CategoryAssignmentRepository categoryAssignmentRepository,
		OperationHistoryRepository operationHistoryRepository,
		MediaCommandService mediaCommandService,
		MediaReadService mediaReadService,
		MediaLifecycleService mediaLifecycleService,
		ObjectMapper objectMapper
	) {
		this.permissionService = permissionService;
		this.doctorRepository = doctorRepository;
		this.hospitalRepository = hospitalRepository;
		this.categoryRepository = categoryRepository;
		this.categoryUsageRepository = categoryUsageRepository;
		this.categoryAssignmentRepository = categoryAssignmentRepository;
		this.operationHistoryRepository = operationHistoryRepository;
		this.mediaCommandService = mediaCommandService;
		this.mediaReadService = mediaReadService;
		this.mediaLifecycleService = mediaLifecycleService;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<DoctorListItemResult> list(AuthenticatedActor actor, SearchDoctorsQuery condition) {
		permissionService.requireStaffPermission(actor, PERMISSION_SHOW);
		validateMetricRange(condition);
		Page<Doctor> page = doctorRepository.findAll(
			specification(condition),
			PageRequest.of(
				Math.max(condition.page(), 1) - 1,
				Math.min(Math.max(condition.perPage(), 1), 100),
				sort(condition)
			)
		);
		List<Doctor> doctors = page.getContent();
		Map<Long, List<DoctorCategoryResult>> categories = categoriesByDoctorIds(doctors);
		Map<Long, MediaResult> profileImages = mediaReadService.primaries(
			MediaOwnerType.DOCTOR,
			doctorIds(doctors),
			MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE
		);

		return PaginatedResponse.from(page, doctor -> toListItem(
			doctor,
			categories.getOrDefault(doctor.id(), List.of()),
			profileImages.get(doctor.id())
		));
	}

	@Transactional(readOnly = true)
	public DoctorDetailResult get(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, PERMISSION_SHOW);
		return toDetail(findActiveDoctor(id));
	}

	@Transactional(readOnly = true)
	public List<DoctorHospitalOptionResult> hospitalOptions(AuthenticatedActor actor, String q, int limit) {
		permissionService.requireStaffPermission(actor, PERMISSION_SHOW);
		String keyword = trimToNull(q);
		Specification<Hospital> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.isNull(root.get("deletedAt")));
			if (keyword != null) {
				var registration = root.join("businessRegistration", JoinType.LEFT);
				List<Predicate> matches = new ArrayList<>();
				matches.add(builder.like(root.get("name"), "%" + keyword + "%"));
				String businessNumber = normalizeLicenseNumber(keyword);
				if (!businessNumber.isEmpty()) {
					matches.add(builder.like(registration.get("businessNumber"), "%" + businessNumber + "%"));
				}
				parseLong(keyword).ifPresent(id -> matches.add(builder.equal(root.get("id"), id)));
				predicates.add(builder.or(matches.toArray(Predicate[]::new)));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
		return hospitalRepository.findAll(
			specification,
			PageRequest.of(0, Math.min(Math.max(limit, 1), 50), Sort.by("name").ascending())
		).stream().map(hospital -> new DoctorHospitalOptionResult(
			hospital.id(),
			hospital.name(),
			hospital.businessRegistration() == null ? null : hospital.businessRegistration().businessNumber()
		)).toList();
	}

	@Transactional
	public DoctorDetailResult create(AuthenticatedActor actor, SaveDoctorCommand command) {
		permissionService.requireStaffPermission(actor, PERMISSION_CREATE);
		Hospital hospital = findLockedHospital(command.hospitalId());
		String licenseNumber = normalizeRequiredLicenseNumber(command.licenseNumber());
		ensureLicenseAvailable(licenseNumber, null);
		DoctorValues values = validateValues(command);

		Doctor doctor = new Doctor(
			hospital,
			command.sortOrder() == null ? 0 : command.sortOrder(),
			values.name(),
			values.gender(),
			values.position(),
			command.careerStartedAt(),
			licenseNumber,
			command.specialistField(),
			values.educationsJson(),
			values.careersJson(),
			values.etcContentsJson(),
			command.status(),
			command.allowStatus()
		);
		Doctor saved = doctorRepository.saveAndFlush(doctor);
		syncCategories(saved.id(), command.categoryIds());
		syncMedia(saved.id(), command, true);
		recordHistory(actor, saved, "CREATED", null, Map.of(), capture(saved));

		return toDetail(saved);
	}

	@Transactional
	public DoctorDetailResult update(AuthenticatedActor actor, Long id, SaveDoctorCommand command) {
		permissionService.requireStaffPermission(actor, PERMISSION_UPDATE);
		Doctor reference = findActiveDoctor(id);
		Map<Long, Hospital> lockedHospitals = lockHospitals(reference.hospitalId(), command.hospitalId());
		Doctor doctor = findLockedDoctor(id);
		Hospital hospital = lockedHospitals.get(command.hospitalId());
		if (hospital == null) {
			throw new ApiException(ErrorCode.NOT_FOUND, "병의원을 찾을 수 없습니다.");
		}
		String licenseNumber = normalizeRequiredLicenseNumber(command.licenseNumber());
		ensureLicenseAvailable(licenseNumber, doctor.id());
		DoctorValues values = validateValues(command);
		Map<String, String> before = capture(doctor);

		doctor.update(
			hospital,
			command.sortOrder() == null ? doctor.sortOrder() : command.sortOrder(),
			values.name(),
			values.gender(),
			values.position(),
			command.careerStartedAt(),
			licenseNumber,
			command.specialistField(),
			values.educationsJson(),
			values.careersJson(),
			values.etcContentsJson(),
			command.status(),
			command.allowStatus()
		);
		syncCategories(doctor.id(), command.categoryIds());
		syncMedia(doctor.id(), command, false);
		recordHistory(actor, doctor, "UPDATED", null, before, capture(doctor));
		return toDetail(doctorRepository.saveAndFlush(doctor));
	}

	@Transactional
	public DoctorDetailResult patch(AuthenticatedActor actor, Long id, PatchDoctorCommand command) {
		permissionService.requireStaffPermission(actor, PERMISSION_UPDATE);
		if (command.status() == null && command.allowStatus() == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "변경할 의료진 상태가 없습니다.");
		}
		Doctor reference = findActiveDoctor(id);
		findLockedHospital(reference.hospitalId());
		Doctor doctor = findLockedDoctor(id);
		Map<String, String> before = capture(doctor);
		if (command.status() != null) {
			doctor.changeStatus(command.status());
		}
		if (command.allowStatus() != null) {
			if (command.allowStatus() == DoctorAllowStatus.REJECTED && trimToNull(command.reason()) == null) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "반려 사유를 입력해주세요.");
			}
			doctor.changeAllowStatus(command.allowStatus());
		}
		recordHistory(actor, doctor, "STATE_UPDATED", trimToNull(command.reason()), before, capture(doctor));
		return toDetail(doctorRepository.saveAndFlush(doctor));
	}

	@Transactional
	public DoctorDeletedResult delete(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, PERMISSION_DELETE);
		Doctor reference = findActiveDoctor(id);
		findLockedHospital(reference.hospitalId());
		Doctor doctor = findLockedDoctor(id);
		doctor.softDelete();
		mediaLifecycleService.softDeleteOwnedMedia(MediaOwnerType.DOCTOR, doctor.id());
		recordHistory(actor, doctor, "DELETED", null, capture(doctor), Map.of());
		doctorRepository.saveAndFlush(doctor);
		return new DoctorDeletedResult(doctor.id(), doctor.deletedAt());
	}

	private Specification<Doctor> specification(SearchDoctorsQuery condition) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.isNull(root.get("deletedAt")));
			predicates.add(builder.isNull(root.get("hospital").get("deletedAt")));
			String keyword = trimToNull(condition.q());
			if (keyword != null) {
				List<Predicate> matches = new ArrayList<>();
				matches.add(builder.like(root.get("name"), "%" + keyword + "%"));
				matches.add(builder.like(root.get("hospital").get("name"), "%" + keyword + "%"));
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
				Subquery<Long> subquery = query.subquery(Long.class);
				var assignment = subquery.from(CategoryAssignment.class);
				subquery.select(assignment.get("categorizableId")).where(
					builder.equal(assignment.get("categorizableType"), CategoryAssignment.DOCTOR_TARGET_TYPE),
					assignment.get("category").get("id").in(condition.categoryIds())
				);
				predicates.add(root.get("id").in(subquery));
			}
			applyMetric(predicates, builder, root.get("careerStartedAt"), condition);
			applyDateRange(predicates, builder, root.get("createdAt"), condition.startDate(), condition.endDate());
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Sort sort(SearchDoctorsQuery condition) {
		Sort.Direction direction = "asc".equalsIgnoreCase(condition.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
		String field = condition.sort();
		if (field == null || "id".equals(field) || "review_count".equals(field) || "consultation_count".equals(field)) {
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
		SearchDoctorsQuery condition
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

	private DoctorValues validateValues(SaveDoctorCommand command) {
		if (command.specialistField() == null || command.status() == null || command.allowStatus() == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "전문의 분류와 상태는 필수입니다.");
		}
		String name = required(command.name(), "의료진명은 필수입니다.");
		String gender = required(command.gender(), "성별은 필수입니다.");
		if (!Set.of("남", "여").contains(gender)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "성별 값이 올바르지 않습니다.");
		}
		String position = required(command.position(), "직책은 필수입니다.");
		if (name.length() > 100 || position.length() > 50) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "의료진명 또는 직책 길이가 제한을 초과했습니다.");
		}
		return new DoctorValues(
			name,
			gender,
			position,
			toJsonList(command.educations(), "학력사항"),
			toJsonList(command.careers(), "경력사항"),
			toJsonList(command.etcContents(), "활동사항")
		);
	}

	private void syncCategories(Long doctorId, List<Long> categoryIds) {
		List<Long> normalizedIds = categoryIds == null ? List.of() : categoryIds.stream().filter(Objects::nonNull).distinct().toList();
		if (normalizedIds.isEmpty() || normalizedIds.size() > MAX_CATEGORY_COUNT) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "진료분야는 1개 이상 5개 이하로 선택해주세요.");
		}
		List<Category> categories = categoryRepository.findByIdIn(normalizedIds);
		Map<Long, Category> categoryMap = categories.stream().collect(Collectors.toMap(Category::id, Function.identity()));
		boolean invalid = categories.size() != normalizedIds.size()
			|| categories.stream().anyMatch(category ->
				category.domain() != CategoryDomain.HOSPITAL_MEDICAL || category.status() != CategoryStatus.ACTIVE
			)
			|| categoryUsageRepository.countByUsageAndStatusAndCategory_IdIn(
				CategoryUsageType.HOSPITAL_DOCTOR_SUBJECT,
				CategoryStatus.ACTIVE,
				normalizedIds
			) != normalizedIds.size();
		if (invalid) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 진료분야 카테고리가 올바르지 않습니다.");
		}
		categoryAssignmentRepository.deleteByCategorizableTypeAndCategorizableId(
			CategoryAssignment.DOCTOR_TARGET_TYPE,
			doctorId
		);
		List<CategoryAssignment> assignments = new ArrayList<>();
		for (int index = 0; index < normalizedIds.size(); index++) {
			assignments.add(new CategoryAssignment(
				CategoryAssignment.DOCTOR_TARGET_TYPE,
				doctorId,
				categoryMap.get(normalizedIds.get(index)),
				index == 0
			));
		}
		categoryAssignmentRepository.saveAll(assignments);
	}

	private void syncMedia(Long doctorId, SaveDoctorCommand command, boolean creating) {
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.DOCTOR,
			doctorId,
			MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE,
			command.profileImage(),
			creating ? null : command.existingProfileImageId(),
			true
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.DOCTOR,
			doctorId,
			MediaCollectionPolicy.DOCTOR_LICENSE_IMAGE,
			command.licenseImage(),
			creating ? null : command.existingLicenseImageId(),
			false
		);
		mediaCommandService.synchronizeSingle(
			MediaOwnerType.DOCTOR,
			doctorId,
			MediaCollectionPolicy.DOCTOR_SPECIALIST_CERTIFICATE_IMAGE,
			command.specialistCertificateImage(),
			creating ? null : command.existingSpecialistCertificateImageId(),
			false
		);
	}

	private DoctorDetailResult toDetail(Doctor doctor) {
		List<DoctorCategoryResult> categories = categoriesByDoctorIds(List.of(doctor))
			.getOrDefault(doctor.id(), List.of());
		return new DoctorDetailResult(
			doctor.id(),
			doctor.hospitalId(),
			doctor.hospital().name(),
			doctor.hospital().businessRegistration() == null
				? null
				: doctor.hospital().businessRegistration().businessNumber(),
			doctor.name(),
			doctor.gender(),
			doctor.position(),
			doctor.careerStartedAt(),
			doctor.licenseNumber(),
			specialist(doctor.specialistField()),
			doctor.status().name(),
			doctor.status().label(),
			doctor.allowStatus().name(),
			doctor.allowStatus().label(),
			fromJsonList(doctor.educations()),
			fromJsonList(doctor.careers()),
			fromJsonList(doctor.etcContents()),
			categories,
			media(mediaReadService.primary(MediaOwnerType.DOCTOR, doctor.id(), MediaCollectionPolicy.DOCTOR_PROFILE_IMAGE)),
			media(mediaReadService.primary(MediaOwnerType.DOCTOR, doctor.id(), MediaCollectionPolicy.DOCTOR_LICENSE_IMAGE)),
			media(mediaReadService.primary(
				MediaOwnerType.DOCTOR,
				doctor.id(),
				MediaCollectionPolicy.DOCTOR_SPECIALIST_CERTIFICATE_IMAGE
			)),
			doctor.viewCount(),
			doctor.createdAt(),
			doctor.updatedAt()
		);
	}

	private DoctorListItemResult toListItem(
		Doctor doctor,
		List<DoctorCategoryResult> categories,
		MediaResult profileImage
	) {
		return new DoctorListItemResult(
			doctor.id(),
			doctor.hospitalId(),
			doctor.hospital().name(),
			doctor.name(),
			doctor.gender(),
			doctor.position(),
			specialist(doctor.specialistField()),
			doctor.careerStartedAt(),
			doctor.licenseNumber(),
			doctor.allowStatus().name(),
			doctor.status().name(),
			0,
			0,
			doctor.createdAt(),
			media(profileImage),
			categories
		);
	}

	private Map<Long, List<DoctorCategoryResult>> categoriesByDoctorIds(List<Doctor> doctors) {
		Set<Long> ids = doctorIds(doctors);
		if (ids.isEmpty()) {
			return Map.of();
		}
		return categoryAssignmentRepository
			.findByCategorizableTypeAndCategorizableIdIn(CategoryAssignment.DOCTOR_TARGET_TYPE, ids)
			.stream()
			.sorted(Comparator.comparing((CategoryAssignment assignment) -> !assignment.primary())
				.thenComparing(assignment -> assignment.category().sortOrder())
				.thenComparing(assignment -> assignment.category().id()))
			.collect(Collectors.groupingBy(
				CategoryAssignment::categorizableId,
				LinkedHashMap::new,
				Collectors.mapping(assignment -> new DoctorCategoryResult(
					assignment.category().id(),
					assignment.category().domain().name(),
					assignment.category().name(),
					assignment.category().fullPath(),
					assignment.primary()
				), Collectors.toList())
			));
	}

	private Set<Long> doctorIds(List<Doctor> doctors) {
		return doctors.stream().map(Doctor::id).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private DoctorMediaResult media(MediaResult media) {
		if (media == null) {
			return null;
		}
		return new DoctorMediaResult(
			media.id(),
			media.contentUrl(),
			media.mimeType(),
			media.size(),
			media.width(),
			media.height(),
			media.metadata()
		);
	}

	private DoctorSpecialistResult specialist(DoctorSpecialistField specialist) {
		return new DoctorSpecialistResult(specialist.code(), specialist.name(), specialist.label());
	}

	private Doctor findActiveDoctor(Long id) {
		return doctorRepository.findByIdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "의료진을 찾을 수 없습니다."));
	}

	private Doctor findLockedDoctor(Long id) {
		return doctorRepository.findForUpdateByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "의료진을 찾을 수 없습니다."));
	}

	private Hospital findLockedHospital(Long id) {
		return hospitalRepository.findForUpdateByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "병의원을 찾을 수 없습니다."));
	}

	private Map<Long, Hospital> lockHospitals(Long firstId, Long secondId) {
		Set<Long> ids = new HashSet<>();
		ids.add(firstId);
		ids.add(secondId);
		Map<Long, Hospital> result = new HashMap<>();
		ids.stream().sorted().forEach(id -> result.put(id, findLockedHospital(id)));
		return result;
	}

	private void ensureLicenseAvailable(String licenseNumber, Long excludedId) {
		boolean exists = excludedId == null
			? doctorRepository.existsByLicenseNumber(licenseNumber)
			: doctorRepository.existsByLicenseNumberAndIdNot(licenseNumber, excludedId);
		if (exists) {
			throw new ApiException(ErrorCode.CONFLICT, "이미 등록된 의사면허 번호입니다.");
		}
	}

	private String normalizeRequiredLicenseNumber(String licenseNumber) {
		String normalized = normalizeLicenseNumber(licenseNumber);
		if (normalized.isEmpty()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "의사면허 번호는 필수입니다.");
		}
		return normalized;
	}

	private String normalizeLicenseNumber(String value) {
		return value == null ? "" : value.replaceAll("\\D", "");
	}

	private String toJsonList(String raw, String fieldName) {
		if (!StringUtils.hasText(raw)) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " JSON 배열은 필수입니다.");
		}
		try {
			List<String> values = objectMapper.readValue(raw, STRING_LIST_TYPE).stream()
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.toList();
			if (values.size() > MAX_TEXT_ITEM_COUNT || values.stream().anyMatch(value -> value.length() > MAX_TEXT_ITEM_LENGTH)) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " 입력 개수 또는 길이가 제한을 초과했습니다.");
			}
			return objectMapper.writeValueAsString(values);
		} catch (JsonProcessingException | NullPointerException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + " JSON 배열이 올바르지 않습니다.");
		}
	}

	private List<String> fromJsonList(String value) {
		if (value == null) {
			return List.of();
		}
		try {
			return objectMapper.readValue(value, STRING_LIST_TYPE);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("저장된 의료진 목록 JSON이 올바르지 않습니다.", exception);
		}
	}

	private void recordHistory(
		AuthenticatedActor actor,
		Doctor doctor,
		String action,
		String reason,
		Map<String, String> before,
		Map<String, String> after
	) {
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_DOCTOR,
			doctor.id(),
			actor.actorType().name(),
			actor.accountId(),
			action,
			reason,
			null
		);
		Set<String> keys = new LinkedHashSet<>();
		keys.addAll(before.keySet());
		keys.addAll(after.keySet());
		for (String key : keys) {
			if (!Objects.equals(before.get(key), after.get(key))) {
				history.addChange(key, before.get(key), after.get(key));
			}
		}
		if (!history.changes().isEmpty() || !"UPDATED".equals(action)) {
			operationHistoryRepository.save(history);
		}
	}

	private Map<String, String> capture(Doctor doctor) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("hospital_id", String.valueOf(doctor.hospitalId()));
		values.put("name", doctor.name());
		values.put("gender", doctor.gender());
		values.put("position", doctor.position());
		values.put("career_started_at", doctor.careerStartedAt() == null ? null : doctor.careerStartedAt().toString());
		values.put("license_number", doctor.licenseNumber());
		values.put("specialist_field", doctor.specialistField().name());
		values.put("status", doctor.status().name());
		values.put("allow_status", doctor.allowStatus().name());
		return values;
	}

	private void validateMetricRange(SearchDoctorsQuery condition) {
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

	private String required(String value, String message) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, message);
		}
		return normalized;
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private record DoctorValues(
		String name,
		String gender,
		String position,
		String educationsJson,
		String careersJson,
		String etcContentsJson
	) {
	}
}
