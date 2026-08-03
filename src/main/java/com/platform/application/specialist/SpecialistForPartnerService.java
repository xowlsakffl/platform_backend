package com.platform.application.specialist;

import com.platform.application.auth.OwnershipPolicy;
import com.platform.application.specialist.command.SaveSpecialistCommand;
import com.platform.application.specialist.command.UpdateSpecialistForPartnerCommand;
import com.platform.application.specialist.query.SearchSpecialistsForPartnerQuery;
import com.platform.application.specialist.result.SpecialistDeletedResult;
import com.platform.application.specialist.result.SpecialistDetailResult;
import com.platform.application.specialist.result.SpecialistListItemResult;
import com.platform.application.media.result.MediaResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AuthenticatedActor;
import com.platform.common.web.PaginatedResponse;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.domain.specialist.SpecialistStatus;
import com.platform.domain.partner.Partner;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
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
public class SpecialistForPartnerService {

	private final OwnershipPolicy ownershipPolicy;
	private final SpecialistRepository specialistRepository;
	private final PartnerRepository partnerRepository;
	private final SpecialistWriteService specialistWriteService;
	private final SpecialistResultAssembler resultAssembler;
	private final SpecialistHistoryService historyService;
	private final SpecialistLifecycleService lifecycleService;

	public SpecialistForPartnerService(
		OwnershipPolicy ownershipPolicy,
		SpecialistRepository specialistRepository,
		PartnerRepository partnerRepository,
		SpecialistWriteService specialistWriteService,
		SpecialistResultAssembler resultAssembler,
		SpecialistHistoryService historyService,
		SpecialistLifecycleService lifecycleService
	) {
		this.ownershipPolicy = ownershipPolicy;
		this.specialistRepository = specialistRepository;
		this.partnerRepository = partnerRepository;
		this.specialistWriteService = specialistWriteService;
		this.resultAssembler = resultAssembler;
		this.historyService = historyService;
		this.lifecycleService = lifecycleService;
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<SpecialistListItemResult> list(
		AuthenticatedActor actor,
		SearchSpecialistsForPartnerQuery condition
	) {
		Long partnerId = requirePartnerId(actor);
		Page<Specialist> page = specialistRepository.findAll(
			specification(partnerId, condition),
			PageRequest.of(
				Math.max(condition.page(), 1) - 1,
				Math.min(Math.max(condition.perPage(), 1), 100),
				sort(condition)
			)
		);
		List<Specialist> specialists = page.getContent();
		Map<Long, MediaResult> profileImages = resultAssembler.profileImages(specialists);

		return PaginatedResponse.from(page, specialist -> resultAssembler.listItem(
			specialist,
			profileImages.get(specialist.id()),
			SpecialistMediaAccessScope.PARTNER
		));
	}

	@Transactional(readOnly = true)
	public SpecialistDetailResult get(AuthenticatedActor actor, Long id) {
		Long partnerId = requirePartnerId(actor);
		return resultAssembler.detail(findOwnedSpecialist(id, partnerId), SpecialistMediaAccessScope.PARTNER);
	}

	@Transactional
	public SpecialistDetailResult create(AuthenticatedActor actor, SaveSpecialistCommand command) {
		Long partnerId = requirePartnerId(actor);
		Partner partner = findLockedPartner(partnerId);
		Specialist saved = specialistWriteService.create(partner, ownedCommand(command, partnerId));
		historyService.record(actor, saved, "SUBMITTED", null, Map.of(), historyService.capture(saved));
		return resultAssembler.detail(saved, SpecialistMediaAccessScope.PARTNER);
	}

	@Transactional
	public SpecialistDetailResult update(AuthenticatedActor actor, Long id, UpdateSpecialistForPartnerCommand command) {
		Long partnerId = requirePartnerId(actor);
		Partner partner = findLockedPartner(partnerId);
		Specialist specialist = findLockedOwnedSpecialist(id, partnerId);
		Map<String, String> before = historyService.capture(specialist);
		Specialist saved = specialistWriteService.updatePartial(specialist, partner, command);
		historyService.record(actor, saved, "UPDATED", null, before, historyService.capture(saved));
		return resultAssembler.detail(saved, SpecialistMediaAccessScope.PARTNER);
	}

	@Transactional
	public SpecialistDetailResult changeStatus(AuthenticatedActor actor, Long id, SpecialistStatus status) {
		if (status == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "운영 상태는 필수입니다.");
		}
		Long partnerId = requirePartnerId(actor);
		findLockedPartner(partnerId);
		Specialist specialist = findLockedOwnedSpecialist(id, partnerId);
		Map<String, String> before = historyService.capture(specialist);
		specialist.changeStatus(status);
		Specialist saved = specialistRepository.saveAndFlush(specialist);
		historyService.record(actor, saved, "STATUS_UPDATED", null, before, historyService.capture(saved));
		return resultAssembler.detail(saved, SpecialistMediaAccessScope.PARTNER);
	}

	@Transactional
	public SpecialistDeletedResult delete(AuthenticatedActor actor, Long id) {
		Long partnerId = requirePartnerId(actor);
		findLockedPartner(partnerId);
		Specialist specialist = findLockedOwnedSpecialist(id, partnerId);
		Map<String, String> before = historyService.capture(specialist);
		lifecycleService.softDelete(specialist);
		historyService.record(actor, specialist, "DELETED", null, before, Map.of());
		specialistRepository.saveAndFlush(specialist);
		return new SpecialistDeletedResult(specialist.id(), specialist.deletedAt());
	}

	private Specification<Specialist> specification(Long partnerId, SearchSpecialistsForPartnerQuery condition) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(builder.equal(root.get("partner").get("id"), partnerId));
			predicates.add(builder.isNull(root.get("deletedAt")));
			predicates.add(builder.isNull(root.get("partner").get("deletedAt")));
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

	private Sort sort(SearchSpecialistsForPartnerQuery condition) {
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

	private SaveSpecialistCommand ownedCommand(SaveSpecialistCommand command, Long partnerId) {
		return new SaveSpecialistCommand(
			partnerId,
			command.sortOrder(),
			command.name(),
			command.gender(),
			command.position(),
			command.careerStartedAt(),
			command.licenseNumber(),
			command.specialistField(),
			command.status(),
			SpecialistAllowStatus.PENDING,
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

	private Long requirePartnerId(AuthenticatedActor actor) {
		Long partnerId = actor == null ? null : actor.partnerId();
		ownershipPolicy.requirePartnerOwner(actor, partnerId);
		if (partnerId == null) {
			throw new ApiException(ErrorCode.FORBIDDEN);
		}
		return partnerId;
	}

	private Specialist findOwnedSpecialist(Long id, Long partnerId) {
		return specialistRepository
			.findByIdAndPartner_IdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(id, partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "스페셜리스트을 찾을 수 없습니다."));
	}

	private Specialist findLockedOwnedSpecialist(Long id, Long partnerId) {
		return specialistRepository
			.findForUpdateByIdAndPartner_IdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(id, partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "스페셜리스트을 찾을 수 없습니다."));
	}

	private Partner findLockedPartner(Long id) {
		return partnerRepository.findForUpdateByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "파트너을 찾을 수 없습니다."));
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
