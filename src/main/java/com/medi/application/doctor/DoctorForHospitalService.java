package com.medi.application.doctor;

import com.medi.application.auth.OwnershipPolicy;
import com.medi.application.doctor.command.SaveDoctorCommand;
import com.medi.application.doctor.command.UpdateDoctorForHospitalCommand;
import com.medi.application.doctor.query.SearchDoctorsForHospitalQuery;
import com.medi.application.doctor.result.DoctorDeletedResult;
import com.medi.application.doctor.result.DoctorDetailResult;
import com.medi.application.doctor.result.DoctorListItemResult;
import com.medi.application.doctor.result.DoctorListCategoryResult;
import com.medi.application.media.result.MediaResult;
import com.medi.common.error.ApiException;
import com.medi.common.error.ErrorCode;
import com.medi.common.security.AuthenticatedActor;
import com.medi.common.web.PaginatedResponse;
import com.medi.domain.doctor.Doctor;
import com.medi.domain.doctor.DoctorAllowStatus;
import com.medi.domain.doctor.DoctorStatus;
import com.medi.domain.hospital.Hospital;
import com.medi.infrastructure.persistence.doctor.DoctorRepository;
import com.medi.infrastructure.persistence.hospital.HospitalRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DoctorForHospitalService {

	private final OwnershipPolicy ownershipPolicy;
	private final DoctorRepository doctorRepository;
	private final HospitalRepository hospitalRepository;
	private final DoctorWriteService doctorWriteService;
	private final DoctorResultAssembler resultAssembler;
	private final DoctorHistoryService historyService;
	private final DoctorLifecycleService lifecycleService;

	public DoctorForHospitalService(
		OwnershipPolicy ownershipPolicy,
		DoctorRepository doctorRepository,
		HospitalRepository hospitalRepository,
		DoctorWriteService doctorWriteService,
		DoctorResultAssembler resultAssembler,
		DoctorHistoryService historyService,
		DoctorLifecycleService lifecycleService
	) {
		this.ownershipPolicy = ownershipPolicy;
		this.doctorRepository = doctorRepository;
		this.hospitalRepository = hospitalRepository;
		this.doctorWriteService = doctorWriteService;
		this.resultAssembler = resultAssembler;
		this.historyService = historyService;
		this.lifecycleService = lifecycleService;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<DoctorListItemResult> list(
		AuthenticatedActor actor,
		SearchDoctorsForHospitalQuery condition
	) {
		Long hospitalId = requireHospitalId(actor);
		Page<Doctor> page = doctorRepository.findAll(
			specification(hospitalId, condition),
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
			DoctorMediaAccessScope.HOSPITAL
		));
	}

	@Transactional(readOnly = true)
	public DoctorDetailResult get(AuthenticatedActor actor, Long id) {
		Long hospitalId = requireHospitalId(actor);
		return resultAssembler.detail(findOwnedDoctor(id, hospitalId), DoctorMediaAccessScope.HOSPITAL);
	}

	@Transactional
	public DoctorDetailResult create(AuthenticatedActor actor, SaveDoctorCommand command) {
		Long hospitalId = requireHospitalId(actor);
		Hospital hospital = findLockedHospital(hospitalId);
		Doctor saved = doctorWriteService.create(hospital, ownedCommand(command, hospitalId));
		historyService.record(actor, saved, "SUBMITTED", null, Map.of(), historyService.capture(saved));
		return resultAssembler.detail(saved, DoctorMediaAccessScope.HOSPITAL);
	}

	@Transactional
	public DoctorDetailResult update(AuthenticatedActor actor, Long id, UpdateDoctorForHospitalCommand command) {
		Long hospitalId = requireHospitalId(actor);
		Hospital hospital = findLockedHospital(hospitalId);
		Doctor doctor = findLockedOwnedDoctor(id, hospitalId);
		Map<String, String> before = historyService.capture(doctor);
		Doctor saved = doctorWriteService.updatePartial(doctor, hospital, command);
		historyService.record(actor, saved, "UPDATED", null, before, historyService.capture(saved));
		return resultAssembler.detail(saved, DoctorMediaAccessScope.HOSPITAL);
	}

	@Transactional
	public DoctorDetailResult changeStatus(AuthenticatedActor actor, Long id, DoctorStatus status) {
		if (status == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "운영 상태는 필수입니다.");
		}
		Long hospitalId = requireHospitalId(actor);
		findLockedHospital(hospitalId);
		Doctor doctor = findLockedOwnedDoctor(id, hospitalId);
		Map<String, String> before = historyService.capture(doctor);
		doctor.changeStatus(status);
		Doctor saved = doctorRepository.saveAndFlush(doctor);
		historyService.record(actor, saved, "STATUS_UPDATED", null, before, historyService.capture(saved));
		return resultAssembler.detail(saved, DoctorMediaAccessScope.HOSPITAL);
	}

	@Transactional
	public DoctorDeletedResult delete(AuthenticatedActor actor, Long id) {
		Long hospitalId = requireHospitalId(actor);
		findLockedHospital(hospitalId);
		Doctor doctor = findLockedOwnedDoctor(id, hospitalId);
		Map<String, String> before = historyService.capture(doctor);
		lifecycleService.softDelete(doctor);
		historyService.record(actor, doctor, "DELETED", null, before, Map.of());
		doctorRepository.saveAndFlush(doctor);
		return new DoctorDeletedResult(doctor.id(), doctor.deletedAt());
	}

	private Specification<Doctor> specification(Long hospitalId, SearchDoctorsForHospitalQuery condition) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.equal(root.get("hospital").get("id"), hospitalId));
			predicates.add(builder.isNull(root.get("deletedAt")));
			predicates.add(builder.isNull(root.get("hospital").get("deletedAt")));
			String keyword = trimToNull(condition.q());
			if (keyword != null) {
				List<Predicate> matches = new ArrayList<>();
				matches.add(builder.like(root.get("name"), "%" + keyword + "%"));
				String digits = normalizeDigits(keyword);
				if (!digits.isEmpty()) {
					matches.add(builder.like(root.get("licenseNumber"), "%" + digits + "%"));
				}
				parseLong(keyword).ifPresent(id -> matches.add(builder.equal(root.get("id"), id)));
				predicates.add(builder.or(matches.toArray(Predicate[]::new)));
			}
			if (!condition.statuses().isEmpty()) {
				predicates.add(root.get("status").in(condition.statuses()));
			}
			if (!condition.allowStatuses().isEmpty()) {
				predicates.add(root.get("allowStatus").in(condition.allowStatuses()));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private Sort sort(SearchDoctorsForHospitalQuery condition) {
		Sort.Direction direction = "asc".equalsIgnoreCase(condition.direction())
			? Sort.Direction.ASC
			: Sort.Direction.DESC;
		String property = switch (condition.sort() == null ? "id" : condition.sort()) {
			case "name" -> "name";
			case "position" -> "position";
			case "status" -> "status";
			case "allow_status" -> "allowStatus";
			case "sort_order" -> "sortOrder";
			case "created_at" -> "createdAt";
			default -> "id";
		};
		return Sort.by(new Sort.Order(direction, property), Sort.Order.desc("id"));
	}

	private SaveDoctorCommand ownedCommand(SaveDoctorCommand command, Long hospitalId) {
		return new SaveDoctorCommand(
			hospitalId,
			command.sortOrder(),
			command.name(),
			command.gender(),
			command.position(),
			command.careerStartedAt(),
			command.licenseNumber(),
			command.specialistField(),
			command.status(),
			DoctorAllowStatus.PENDING,
			command.categoryIds(),
			command.educations(),
			command.careers(),
			command.etcContents(),
			command.profileImage(),
			command.existingProfileImageId(),
			command.licenseImage(),
			command.existingLicenseImageId(),
			command.specialistCertificateImage(),
			command.existingSpecialistCertificateImageId()
		);
	}

	private Long requireHospitalId(AuthenticatedActor actor) {
		Long hospitalId = actor == null ? null : actor.hospitalId();
		ownershipPolicy.requireHospitalOwner(actor, hospitalId);
		if (hospitalId == null) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
		return hospitalId;
	}

	private Doctor findOwnedDoctor(Long id, Long hospitalId) {
		return doctorRepository
			.findByIdAndHospital_IdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(id, hospitalId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "의료진을 찾을 수 없습니다."));
	}

	private Doctor findLockedOwnedDoctor(Long id, Long hospitalId) {
		return doctorRepository
			.findForUpdateByIdAndHospital_IdAndDeletedAtIsNullAndHospital_DeletedAtIsNull(id, hospitalId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "의료진을 찾을 수 없습니다."));
	}

	private Hospital findLockedHospital(Long id) {
		return hospitalRepository.findForUpdateByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "병원을 찾을 수 없습니다."));
	}

	private String normalizeDigits(String value) {
		return value == null ? "" : value.replaceAll("\\D", "");
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
