package com.medi.application.doctor;

import com.medi.application.auth.PermissionService;
import com.medi.application.doctor.command.UpdateDoctorStatusForStaffCommand;
import com.medi.application.doctor.command.SaveDoctorCommand;
import com.medi.application.doctor.command.UpdateDoctorForStaffCommand;
import com.medi.application.doctor.query.SearchDoctorsForStaffQuery;
import com.medi.application.doctor.result.DoctorDeletedResult;
import com.medi.application.doctor.result.DoctorDetailResult;
import com.medi.application.doctor.result.HospitalOptionForStaffResult;
import com.medi.application.doctor.result.DoctorListItemResult;
import com.medi.application.doctor.result.DoctorListCategoryResult;
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
import com.medi.domain.doctor.Doctor;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorSpecialistField;
import com.medi.domain.hospital.Hospital;
import com.medi.infrastructure.persistence.category.CategoryRepository;
import com.medi.infrastructure.persistence.doctor.DoctorRepository;
import com.medi.infrastructure.persistence.hospital.HospitalRepository;
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
public class DoctorForStaffService {

	private final PermissionService permissionService;
	private final DoctorRepository doctorRepository;
	private final HospitalRepository hospitalRepository;
	private final CategoryRepository categoryRepository;
	private final DoctorWriteService doctorWriteService;
	private final DoctorResultAssembler resultAssembler;
	private final DoctorHistoryService historyService;
	private final DoctorLifecycleService lifecycleService;

	public DoctorForStaffService(
		PermissionService permissionService,
		DoctorRepository doctorRepository,
		HospitalRepository hospitalRepository,
		CategoryRepository categoryRepository,
		DoctorWriteService doctorWriteService,
		DoctorResultAssembler resultAssembler,
		DoctorHistoryService historyService,
		DoctorLifecycleService lifecycleService
	) {
		this.permissionService = permissionService;
		this.doctorRepository = doctorRepository;
		this.hospitalRepository = hospitalRepository;
		this.categoryRepository = categoryRepository;
		this.doctorWriteService = doctorWriteService;
		this.resultAssembler = resultAssembler;
		this.historyService = historyService;
		this.lifecycleService = lifecycleService;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<DoctorListItemResult> list(AuthenticatedActor actor, SearchDoctorsForStaffQuery condition) {
		permissionService.requireStaffPermission(actor, AccessPermissions.DOCTOR_SHOW);
		validateMetricRange(condition);
		Set<Long> expandedCategoryIds = expandCategoryIds(condition.categoryIds());
		Page<Doctor> page = doctorRepository.findAll(
			specification(condition, expandedCategoryIds),
			PageRequest.of(
				Math.max(condition.page(), 1) - 1,
				Math.min(Math.max(condition.perPage(), 1), 100),
				sort(condition)
			)
		);
		List<Doctor> doctors = page.getContent();
		Map<Long, List<DoctorListCategoryResult>> categories = resultAssembler.listCategoriesByDoctorIds(doctors);
		Map<Long, MediaResult> profileImages = resultAssembler.profileImages(doctors);

		return PaginatedResponse.from(page, doctor -> resultAssembler.listItem(
			doctor,
			categories.getOrDefault(doctor.id(), List.of()),
			profileImages.get(doctor.id()),
			DoctorMediaAccessScope.STAFF
		));
	}

	@Transactional(readOnly = true)
	public DoctorDetailResult get(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, AccessPermissions.DOCTOR_SHOW);
		return resultAssembler.detail(findActiveDoctor(id), DoctorMediaAccessScope.STAFF);
	}

	@Transactional(readOnly = true)
	public List<HospitalOptionForStaffResult> hospitalOptions(AuthenticatedActor actor, String q, int limit) {
		permissionService.requireStaffPermission(actor, AccessPermissions.HOSPITAL_SHOW);
		String keyword = trimToNull(q);
		Specification<Hospital> specification = (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.isNull(root.get("deletedAt")));
			if (keyword != null) {
				predicates.add(builder.like(root.get("name"), escapeLike(keyword) + "%", '\\'));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
		return hospitalRepository.findAll(
			specification,
			PageRequest.of(
				0,
				Math.min(Math.max(limit, 1), 20),
				Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))
			)
		).stream().map(hospital -> new HospitalOptionForStaffResult(
			hospital.id(),
			hospital.name(),
			hospital.businessRegistration() == null ? null : hospital.businessRegistration().businessNumber()
		)).toList();
	}

	@Transactional
	public DoctorDetailResult create(AuthenticatedActor actor, SaveDoctorCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.DOCTOR_CREATE);
		Hospital hospital = findLockedHospital(command.hospitalId());
		Doctor saved = doctorWriteService.create(hospital, command);
		historyService.record(actor, saved, "CREATED", null, Map.of(), historyService.capture(saved));

		return resultAssembler.detail(saved, DoctorMediaAccessScope.STAFF);
	}

	@Transactional
	public DoctorDetailResult update(AuthenticatedActor actor, Long id, UpdateDoctorForStaffCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.DOCTOR_UPDATE);
		Doctor reference = findActiveDoctor(id);
		Long targetHospitalId = command.specified("hospital_id") ? command.hospitalId() : reference.hospitalId();
		if (targetHospitalId == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "병원은 필수입니다.");
		}
		if (command.specified("allow_status")
			&& command.allowStatus() == DoctorAllowStatus.REJECTED
			&& trimToNull(command.reason()) == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "반려 사유를 입력해주세요.");
		}
		Map<Long, Hospital> lockedHospitals = lockHospitals(reference.hospitalId(), targetHospitalId);
		Doctor doctor = findLockedDoctor(id);
		Hospital hospital = lockedHospitals.get(targetHospitalId);
		if (hospital == null) {
			throw new ApiException(ErrorCode.NOT_FOUND, "병원을 찾을 수 없습니다.");
		}
		Map<String, String> before = historyService.capture(doctor);
		Doctor saved = doctorWriteService.updatePartial(doctor, hospital, command);
		String reason = command.specified("allow_status")
			&& !Objects.equals(before.get("allow_status"), saved.allowStatus().name())
			? trimToNull(command.reason())
			: null;
		historyService.record(actor, saved, "UPDATED", reason, before, historyService.capture(saved));
		return resultAssembler.detail(saved, DoctorMediaAccessScope.STAFF);
	}

	@Transactional
	public DoctorDetailResult patch(AuthenticatedActor actor, Long id, UpdateDoctorStatusForStaffCommand command) {
		permissionService.requireStaffPermission(actor, AccessPermissions.DOCTOR_UPDATE);
		if (command.status() == null && command.allowStatus() == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "변경할 의료진 상태가 없습니다.");
		}
		Doctor reference = findActiveDoctor(id);
		findLockedHospital(reference.hospitalId());
		Doctor doctor = findLockedDoctor(id);
		Map<String, String> before = historyService.capture(doctor);
		if (command.status() != null) {
			doctor.changeStatus(command.status());
		}
		if (command.allowStatus() != null) {
			if (command.allowStatus() == DoctorAllowStatus.REJECTED && trimToNull(command.reason()) == null) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "반려 사유를 입력해주세요.");
			}
			doctor.changeAllowStatus(command.allowStatus());
		}
		historyService.record(
			actor,
			doctor,
			"STATE_UPDATED",
			trimToNull(command.reason()),
			before,
			historyService.capture(doctor)
		);
		return resultAssembler.detail(doctorRepository.saveAndFlush(doctor), DoctorMediaAccessScope.STAFF);
	}

	@Transactional
	public DoctorDeletedResult delete(AuthenticatedActor actor, Long id) {
		permissionService.requireStaffPermission(actor, AccessPermissions.DOCTOR_DELETE);
		Doctor reference = findActiveDoctor(id);
		findLockedHospital(reference.hospitalId());
		Doctor doctor = findLockedDoctor(id);
		Map<String, String> before = historyService.capture(doctor);
		lifecycleService.softDelete(doctor);
		historyService.record(actor, doctor, "DELETED", null, before, Map.of());
		doctorRepository.saveAndFlush(doctor);
		return new DoctorDeletedResult(doctor.id(), doctor.deletedAt());
	}

	private Specification<Doctor> specification(SearchDoctorsForStaffQuery condition, Set<Long> expandedCategoryIds) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.isNull(root.get("deletedAt")));
			predicates.add(builder.isNull(root.get("hospital").get("deletedAt")));
			if (condition.hospitalId() != null) {
				predicates.add(builder.equal(root.get("hospital").get("id"), condition.hospitalId()));
			}
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
				if (expandedCategoryIds.isEmpty()) {
					predicates.add(builder.disjunction());
					return builder.and(predicates.toArray(Predicate[]::new));
				}
				Subquery<Long> subquery = query.subquery(Long.class);
				var assignment = subquery.from(CategoryAssignment.class);
				subquery.select(assignment.get("categorizableId")).where(
					builder.equal(assignment.get("categorizableType"), CategoryAssignment.DOCTOR_TARGET_TYPE),
					assignment.get("category").get("id").in(expandedCategoryIds)
				);
				predicates.add(root.get("id").in(subquery));
			}
			applyMetric(predicates, builder, root.get("careerStartedAt"), condition);
			applyDateRange(predicates, builder, root.get("createdAt"), condition.startDate(), condition.endDate());
			if (condition.sort() == null && !Long.class.equals(query.getResultType())) {
				query.orderBy(
					builder.asc(builder.selectCase()
						.when(builder.equal(root.get("allowStatus"), DoctorAllowStatus.PENDING), 0)
						.otherwise(1)),
					builder.desc(root.get("createdAt")),
					builder.desc(root.get("id"))
				);
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Sort sort(SearchDoctorsForStaffQuery condition) {
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
		SearchDoctorsForStaffQuery condition
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
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "병원을 찾을 수 없습니다."));
	}

	private Map<Long, Hospital> lockHospitals(Long firstId, Long secondId) {
		Set<Long> ids = new HashSet<>();
		ids.add(firstId);
		ids.add(secondId);
		Map<Long, Hospital> result = new HashMap<>();
		ids.stream().sorted().forEach(id -> result.put(id, findLockedHospital(id)));
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
			|| selected.stream().anyMatch(category -> !CategoryAssignmentTarget.DOCTOR.accepts(category));
		if (invalid) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "선택한 진료분야 카테고리가 올바르지 않습니다.");
		}

		Set<Long> expanded = new LinkedHashSet<>(selectedIds);
		for (Category category : selected) {
			String path = StringUtils.hasText(category.fullPath()) ? category.fullPath() : category.name();
			expanded.addAll(categoryRepository
				.findByDomainAndGroupCodeAndFullPathStartingWithOrderByDepthAsc(
					CategoryDomain.MEDICAL,
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

	private void validateMetricRange(SearchDoctorsForStaffQuery condition) {
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
