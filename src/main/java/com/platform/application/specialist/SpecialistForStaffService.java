package com.platform.application.specialist;

import com.platform.application.auth.PermissionService;
import com.platform.application.media.result.MediaResult;
import com.platform.application.specialist.command.SaveSpecialistCommand;
import com.platform.application.specialist.command.ReorderSpecialistsForStaffCommand;
import com.platform.application.specialist.command.UpdateSpecialistForStaffCommand;
import com.platform.application.specialist.command.UpdateSpecialistStatusForStaffCommand;
import com.platform.application.specialist.result.SpecialistDeletedResult;
import com.platform.application.specialist.result.SpecialistDetailResult;
import com.platform.application.specialist.result.SpecialistListItemResult;
import com.platform.application.specialist.result.SpecialistOrderResult;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.common.security.AccessPermissions;
import com.platform.common.security.AuthenticatedActor;
import com.platform.domain.account.AccountStaff;
import com.platform.domain.partner.Partner;
import com.platform.domain.specialist.Specialist;
import com.platform.domain.specialist.SpecialistAllowStatus;
import com.platform.infrastructure.persistence.account.AccountStaffRepository;
import com.platform.infrastructure.persistence.partner.PartnerRepository;
import com.platform.infrastructure.persistence.specialist.SpecialistRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SpecialistForStaffService {

	private final PermissionService permissionService;
	private final SpecialistRepository specialistRepository;
	private final PartnerRepository partnerRepository;
	private final AccountStaffRepository accountStaffRepository;
	private final SpecialistWriteService specialistWriteService;
	private final SpecialistResultAssembler resultAssembler;
	private final SpecialistHistoryService historyService;
	private final SpecialistLifecycleService lifecycleService;

	public SpecialistForStaffService(
		PermissionService permissionService,
		SpecialistRepository specialistRepository,
		PartnerRepository partnerRepository,
		AccountStaffRepository accountStaffRepository,
		SpecialistWriteService specialistWriteService,
		SpecialistResultAssembler resultAssembler,
		SpecialistHistoryService historyService,
		SpecialistLifecycleService lifecycleService
	) {
		this.permissionService = permissionService;
		this.specialistRepository = specialistRepository;
		this.partnerRepository = partnerRepository;
		this.accountStaffRepository = accountStaffRepository;
		this.specialistWriteService = specialistWriteService;
		this.resultAssembler = resultAssembler;
		this.historyService = historyService;
		this.lifecycleService = lifecycleService;
	}

	@Transactional(readOnly = true)
	public List<SpecialistListItemResult> list(AuthenticatedActor actor, Long partnerId) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_SHOW);
		ensureActivePartner(partnerId);
		List<Specialist> specialists = specialistRepository
			.findByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId);
		Map<Long, MediaResult> profileImages = resultAssembler.profileImages(specialists);
		Map<Long, Long> optionCounts = resultAssembler.optionCounts(specialists);
		return specialists.stream()
			.map(specialist -> resultAssembler.listItem(
				specialist,
				profileImages.get(specialist.id()),
				optionCounts.getOrDefault(specialist.id(), 0L),
				SpecialistMediaAccessScope.STAFF
			))
			.toList();
	}

	@Transactional(readOnly = true)
	public SpecialistDetailResult get(AuthenticatedActor actor, Long partnerId, Long specialistId) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_SHOW);
		return resultAssembler.detail(
			findOwnedSpecialist(partnerId, specialistId),
			SpecialistMediaAccessScope.STAFF
		);
	}

	@Transactional
	public SpecialistDetailResult create(
		AuthenticatedActor actor,
		Long partnerId,
		SaveSpecialistCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_CREATE);
		Partner partner = findLockedPartner(partnerId);
		Specialist saved = specialistWriteService.create(partner, command);
		saved.requestReview();
		saved = specialistRepository.saveAndFlush(saved);
		historyService.record(actor, saved, "CREATED", null, Map.of(), historyService.capture(saved));
		return resultAssembler.detail(saved, SpecialistMediaAccessScope.STAFF);
	}

	@Transactional
	public SpecialistDetailResult update(
		AuthenticatedActor actor,
		Long partnerId,
		Long specialistId,
		UpdateSpecialistForStaffCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_UPDATE);
		Partner partner = findLockedPartner(partnerId);
		Specialist specialist = findLockedOwnedSpecialist(partnerId, specialistId);
		if (command.specified("allow_status")
			&& command.allowStatus() == SpecialistAllowStatus.REJECTED
			&& trimToNull(command.reason()) == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "반려 사유를 입력해주세요.");
		}
		boolean allowStatusChanged = command.specified("allow_status")
			&& command.allowStatus() != null
			&& command.allowStatus() != specialist.allowStatus();
		if (allowStatusChanged) {
			assertAllowStatusTransition(specialist.allowStatus(), command.allowStatus());
		}
		Map<String, String> before = historyService.capture(specialist);
		Specialist saved = specialistWriteService.updatePartial(specialist, partner, command);
		if (allowStatusChanged) {
			applyAllowStatus(saved, command.allowStatus(), actor);
			saved = specialistRepository.saveAndFlush(saved);
		}
		String reason = command.specified("allow_status")
			&& !Objects.equals(before.get("allow_status"), saved.allowStatus().name())
			? trimToNull(command.reason())
			: null;
		historyService.record(actor, saved, "UPDATED", reason, before, historyService.capture(saved));
		return resultAssembler.detail(saved, SpecialistMediaAccessScope.STAFF);
	}

	@Transactional
	public SpecialistDetailResult patch(
		AuthenticatedActor actor,
		Long partnerId,
		Long specialistId,
		UpdateSpecialistStatusForStaffCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_UPDATE);
		if (command.status() == null && command.allowStatus() == null) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "변경할 전문가 상태가 없습니다.");
		}
		findLockedPartner(partnerId);
		Specialist specialist = findLockedOwnedSpecialist(partnerId, specialistId);
		Map<String, String> before = historyService.capture(specialist);
		if (command.status() != null) {
			specialist.changeStatus(command.status());
		}
		if (command.allowStatus() != null && specialist.allowStatus() != command.allowStatus()) {
			if (command.allowStatus() == SpecialistAllowStatus.REJECTED && trimToNull(command.reason()) == null) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "반려 사유를 입력해주세요.");
			}
			assertAllowStatusTransition(specialist.allowStatus(), command.allowStatus());
			applyAllowStatus(specialist, command.allowStatus(), actor);
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
	public SpecialistOrderResult reorder(
		AuthenticatedActor actor,
		Long partnerId,
		ReorderSpecialistsForStaffCommand command
	) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_UPDATE);
		findLockedPartner(partnerId);
		List<Specialist> specialists = specialistRepository
			.findForUpdateByPartner_IdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(partnerId);
		List<Long> requestedIds = command.specialistIds();
		if (requestedIds.size() != specialists.size()
			|| new HashSet<>(requestedIds).size() != requestedIds.size()) {
			throw invalidSpecialistOrder();
		}

		Map<Long, Specialist> specialistsById = specialists.stream()
			.collect(java.util.stream.Collectors.toMap(Specialist::id, specialist -> specialist));
		if (!specialistsById.keySet().equals(new HashSet<>(requestedIds))) {
			throw invalidSpecialistOrder();
		}

		for (int index = 0; index < requestedIds.size(); index++) {
			specialistsById.get(requestedIds.get(index)).changeSortOrder(index);
		}
		specialistRepository.flush();
		return new SpecialistOrderResult(List.copyOf(requestedIds));
	}

	@Transactional
	public SpecialistDeletedResult delete(AuthenticatedActor actor, Long partnerId, Long specialistId) {
		permissionService.requireStaffPermission(actor, AccessPermissions.SPECIALIST_DELETE);
		findLockedPartner(partnerId);
		Specialist specialist = findLockedOwnedSpecialist(partnerId, specialistId);
		Map<String, String> before = historyService.capture(specialist);
		lifecycleService.softDelete(specialist);
		historyService.record(actor, specialist, "DELETED", null, before, Map.of());
		specialistRepository.saveAndFlush(specialist);
		return new SpecialistDeletedResult(specialist.id(), specialist.deletedAt());
	}

	private Specialist findOwnedSpecialist(Long partnerId, Long specialistId) {
		return specialistRepository
			.findByIdAndPartner_IdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(specialistId, partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "전문가를 찾을 수 없습니다."));
	}

	private Specialist findLockedOwnedSpecialist(Long partnerId, Long specialistId) {
		return specialistRepository
			.findForUpdateByIdAndPartner_IdAndDeletedAtIsNullAndPartner_DeletedAtIsNull(specialistId, partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "전문가를 찾을 수 없습니다."));
	}

	private void ensureActivePartner(Long partnerId) {
		if (!partnerRepository.existsByIdAndDeletedAtIsNull(partnerId)) {
			throw new ApiException(ErrorCode.NOT_FOUND, "업체를 찾을 수 없습니다.");
		}
	}

	private Partner findLockedPartner(Long partnerId) {
		return partnerRepository.findForUpdateByIdAndDeletedAtIsNull(partnerId)
			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업체를 찾을 수 없습니다."));
	}

	private void assertAllowStatusTransition(SpecialistAllowStatus before, SpecialistAllowStatus after) {
		if (before != after && !before.canTransitionTo(after)) {
			throw new ApiException(
				ErrorCode.INVALID_REQUEST,
				"검수 상태는 검수 신청 → 검수 중 → 승인/반려 순서로만 변경할 수 있습니다."
			);
		}
	}

	private void applyAllowStatus(
		Specialist specialist,
		SpecialistAllowStatus allowStatus,
		AuthenticatedActor actor
	) {
		switch (allowStatus) {
			case REVIEW_REQUESTED -> specialist.requestReview();
			case IN_REVIEW -> specialist.startReview(activeReviewStaff(actor));
			case APPROVED, REJECTED -> specialist.completeReview(allowStatus);
		}
	}

	private AccountStaff activeReviewStaff(AuthenticatedActor actor) {
		return accountStaffRepository.findByIdAndDeletedAtIsNull(actor.accountId())
			.filter(AccountStaff::isActive)
			.orElseThrow(() -> new ApiException(
				ErrorCode.FORBIDDEN,
				"검수를 시작할 활성 Staff 계정을 찾을 수 없습니다."
			));
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private ApiException invalidSpecialistOrder() {
		return new ApiException(ErrorCode.INVALID_REQUEST, "전문가 순서가 현재 업체 목록과 일치하지 않습니다.");
	}
}
